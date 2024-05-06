package io.wobline.payments.application;

import static io.wobline.payments.application.PaymentEntity.PAYMENT_ENTITY_TYPE_KEY;

import io.vavr.control.Option;
import io.wobline.payments.base.domain.Clock;
import io.wobline.payments.domain.*;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.cluster.sharding.typed.javadsl.Entity;
import org.apache.pekko.cluster.sharding.typed.javadsl.EntityRef;

public class PaymentService {

    private final ClusterSharding sharding;
    private Duration askTimeout = Duration.ofSeconds(5); // TODO should be configurable

    public PaymentService(ClusterSharding sharding, Clock clock) {
        this.sharding = sharding;
        sharding.init(
                Entity.of(
                        PAYMENT_ENTITY_TYPE_KEY,
                        entityContext -> PaymentEntity.create(entityContext.getEntityId(), clock)));
    }

    public CompletionStage<PaymentEntityResponse> processPayment(
            String cardNumber,
            String expiryDate,
            String cvv,
            Double amount,
            String currency,
            String merchantId) {
        PaymentId paymentId = PaymentId.of(merchantId, cardNumber, amount, currency);

        return processCommand(
                new PaymentCommand.ProcessPayment(
                        paymentId, cardNumber, expiryDate, cvv, amount, currency, merchantId));
    }

    public CompletionStage<Option<Payment>> fetch(PaymentId paymentId) {
        return getPaymentEntityRef(paymentId).ask(PaymentEntityCommand.GetPayment::new, askTimeout);
    }

    private CompletionStage<PaymentEntityResponse> processCommand(PaymentCommand command) {
        return getPaymentEntityRef(command.paymentId())
                .ask(
                        replyTo -> new PaymentEntityCommand.PaymentCommandEnvelope(command, replyTo),
                        askTimeout);
    }

    private EntityRef<PaymentEntityCommand> getPaymentEntityRef(PaymentId paymentId) {
        return sharding.entityRefFor(PAYMENT_ENTITY_TYPE_KEY, paymentId.id().toString());
    }
}
