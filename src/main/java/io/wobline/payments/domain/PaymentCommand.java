package io.wobline.payments.domain;

import java.io.Serializable;

public sealed interface PaymentCommand extends Serializable {
    PaymentId paymentId();

    record ProcessPayment(PaymentId paymentId, String cardNumber, String expiryDate,
                          String cvv, Double amount, String currency,
                          String merchantId) implements PaymentCommand {
    }

    record UpdatePaymentStatus(PaymentId paymentId, String cardNumber, String expiryDate,
                               String cvv, Double amount, String currency,
                               String merchantId, PaymentStatus status) implements PaymentCommand {
    }
}
