package io.wobline.payments.api;

import io.wobline.payments.domain.Payment;
import io.wobline.payments.domain.PaymentStatus;

import java.util.UUID;

public record PaymentResponse(
        String timestamp,
        UUID id,
        String cardNumber,
        String expiryDate,
        String cvv,
        Double amount,
        String currency,
        String merchantId,
        PaymentStatus status) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.timestamp(),
                payment.paymentId().id(),
                payment.cardNumber(),
                payment.expiryDate(),
                payment.cvv(),
                payment.amount(),
                payment.currency(),
                payment.merchantId(),
                payment.status());
    }
}
