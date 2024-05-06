package io.wobline.payments.domain;

import java.io.Serializable;
import java.time.Instant;

public sealed interface PaymentEvent extends Serializable {
    PaymentId paymentId();

    Instant createdAt();

    record PaymentProcessed(
            PaymentId paymentId,
            Instant createdAt,
            String cardNumber,
            String expiryDate,
            String cvv,
            Double amount,
            String currency,
            String merchantId,
            PaymentStatus status)
            implements PaymentEvent {
    }

    record PaymentStatusUpdated(
            PaymentId paymentId,
            Instant createdAt,
            String cardNumber,
            String expiryDate,
            String cvv,
            Double amount,
            String currency,
            String merchantId,
            PaymentStatus status)
            implements PaymentEvent {
    }
}
