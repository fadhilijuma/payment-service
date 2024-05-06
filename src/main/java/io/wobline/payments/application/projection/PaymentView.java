package io.wobline.payments.application.projection;

import io.wobline.payments.domain.PaymentStatus;

public record PaymentView(
    String paymentId,
    String cardNumber,
    String expiryDate,
    String cvv,
    Double amount,
    String currency,
    String merchantId,
    PaymentStatus status) {}
