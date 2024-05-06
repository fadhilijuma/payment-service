package io.wobline.payments.application;

import io.vavr.control.Option;
import io.wobline.payments.domain.Payment;
import io.wobline.payments.domain.PaymentCommand;
import java.io.Serializable;
import org.apache.pekko.actor.typed.ActorRef;

public sealed interface PaymentEntityCommand extends Serializable {

    record PaymentCommandEnvelope(PaymentCommand command, ActorRef<PaymentEntityResponse> replyTo) implements PaymentEntityCommand {
    }
    record PaymentUpdateEnvelope(PaymentCommand command) implements PaymentEntityCommand {
    }

    record GetPayment(ActorRef<Option<Payment>> replyTo) implements PaymentEntityCommand {
    }
}
