package io.wobline.payments.application;

import io.wobline.payments.domain.PaymentCommandError;

import java.io.Serializable;

public sealed interface PaymentEntityResponse extends Serializable {

    final class CommandProcessed implements PaymentEntityResponse {
    }

    record CommandRejected(PaymentCommandError error) implements PaymentEntityResponse {
    }
}
