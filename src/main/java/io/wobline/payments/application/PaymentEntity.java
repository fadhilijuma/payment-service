package io.wobline.payments.application;

import static io.wobline.payments.domain.PaymentCommandError.PAYMENT_ALREADY_EXISTS;

import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.wobline.payments.base.domain.Clock;
import io.wobline.payments.domain.*;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.apache.pekko.Done;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityTypeKey;
import org.apache.pekko.persistence.typed.PersistenceId;
import org.apache.pekko.persistence.typed.javadsl.*;

public class PaymentEntity extends EventSourcedBehaviorWithEnforcedReplies<PaymentEntityCommand, PaymentEvent, Payment> {

    public static final EntityTypeKey<PaymentEntityCommand> PAYMENT_ENTITY_TYPE_KEY =
            EntityTypeKey.create(PaymentEntityCommand.class, "PaymentTransaction");
    public static final String PAYMENT_EVENT_TAG = "PaymentEvent";

    private final Clock clock;
    private final ActorContext<PaymentEntityCommand> context;

    private PaymentEntity(PersistenceId persistenceId, Clock clock, ActorContext<PaymentEntityCommand> context) {
        super(persistenceId);
        this.clock = clock;
        this.context = context;
    }

    public static PersistenceId persistenceId(String uniqueId) {
        return PersistenceId.of(PAYMENT_ENTITY_TYPE_KEY.name(), uniqueId);
    }

    public static Behavior<PaymentEntityCommand> create(String uniqueId,
                                                        Clock clock) {
        return Behaviors.setup(context -> {
            PersistenceId persistenceId = PaymentEntity.persistenceId(uniqueId);
            context.getLog().info("PaymentTransactionEntity {} initialization started", uniqueId);
            return new PaymentEntity(persistenceId, clock, context);
        });
    }

    @Override
    public Payment emptyState() {
        return null;
    }

    @Override
    public CommandHandlerWithReply<PaymentEntityCommand, PaymentEvent, Payment> commandHandler() {
        var builder = newCommandHandlerWithReplyBuilder();

        builder.forNullState()
                .onCommand(PaymentEntityCommand.GetPayment.class, this::returnEmptyState)
                .onCommand(PaymentEntityCommand.PaymentCommandEnvelope.class, this::handlePaymentProcessingCommand);

        builder.forStateType(Payment.class)
                .onCommand(PaymentEntityCommand.GetPayment.class, this::returnState)
                .onCommand(PaymentEntityCommand.PaymentUpdateEnvelope.class, this::handleUpdateCommand);

        return builder.build();
    }

    private ReplyEffect<PaymentEvent, Payment> handlePaymentProcessingCommand(PaymentEntityCommand.PaymentCommandEnvelope envelope) {
        PaymentCommand command = envelope.command();
        if (command instanceof PaymentCommand.ProcessPayment processPayment) {
            Either<PaymentCommandError, List<PaymentEvent>> processingResult = PaymentProcessor.process(processPayment, clock).map(List::of);
            return handleResult(envelope, processingResult);
        } else {
            context.getLog().warn("Payment {} not created", command.paymentId());
            return Effect().reply(envelope.replyTo(), new PaymentEntityResponse.CommandRejected(PAYMENT_ALREADY_EXISTS));
        }
    }

    private ReplyEffect<PaymentEvent, Payment> handleResult(PaymentEntityCommand envelope, Either<PaymentCommandError, List<PaymentEvent>> processingResult) {
        if (envelope instanceof PaymentEntityCommand.PaymentCommandEnvelope commandEnvelope) {
            PaymentCommand command = commandEnvelope.command();
            return processingResult.fold(
                    error -> {
                        context.getLog().info("Command rejected: {} with {}", command, error);
                        return Effect()
                                .reply(commandEnvelope.replyTo(), new PaymentEntityResponse.CommandRejected(error));
                    },
                    events -> {
                        context.getLog().debug("Command handled: {}", command);
                        return Effect().persist(events.toJavaList())
                                .thenRun(() -> context.pipeToSelf(futureResult(), (ok, exc) -> {
                                    PaymentCommand.ProcessPayment processPayment = (PaymentCommand.ProcessPayment) command;
                                    PaymentCommand.UpdatePaymentStatus updateCommand = new PaymentCommand.UpdatePaymentStatus(processPayment.paymentId(), processPayment.cardNumber(), processPayment.expiryDate(), processPayment.cvv(), processPayment.amount(), processPayment.currency(), processPayment.merchantId(), PaymentStatus.PENDING);

                                    return new PaymentEntityCommand.PaymentUpdateEnvelope(updateCommand);

                                }))
                                .thenReply(commandEnvelope.replyTo(), s -> new PaymentEntityResponse.CommandProcessed());
                    });
        }
        return processingResult.fold(
                error -> Effect()
                        .noReply(),
                events -> Effect().persist(events.toJavaList())
                        .thenNoReply());

    }


    private ReplyEffect<PaymentEvent, Payment> handleUpdateCommand(Payment payment, PaymentEntityCommand.PaymentUpdateEnvelope envelope) {
        Either<PaymentCommandError, List<PaymentEvent>> processingResult = payment.process(envelope.command(), clock);
        return handleResult(envelope, processingResult);
    }

    private ReplyEffect<PaymentEvent, Payment> returnEmptyState(PaymentEntityCommand.GetPayment getPayment) {
        return Effect().reply(getPayment.replyTo(), Option.none());
    }

    private ReplyEffect<PaymentEvent, Payment> returnState(Payment payment, PaymentEntityCommand.GetPayment getPayment) {
        return Effect().reply(getPayment.replyTo(), Option.of(payment));
    }

    public CompletionStage<Done> futureResult() {
        CompletableFuture<Done> future = new CompletableFuture<>();
        CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS)
                .execute(() -> {
                    future.complete(Done.getInstance());
                });

        return future;
    }

    @Override
    public EventHandler<Payment, PaymentEvent> eventHandler() {
        EventHandlerBuilder<Payment, PaymentEvent> builder = newEventHandlerBuilder();

        builder.forNullState()
                .onEvent(PaymentEvent.PaymentProcessed.class, paymentProcessed -> Payment.create(paymentProcessed, PaymentStatus.PENDING));

        builder.forStateType(Payment.class)
                .onAnyEvent(Payment::apply);

        return builder.build();
    }


    @Override
    public Set<String> tagsFor(PaymentEvent paymentEvent) {
        return Set.of(PAYMENT_EVENT_TAG);
    }
}
