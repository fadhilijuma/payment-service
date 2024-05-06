package io.wobline.payments.application.projection;

import io.wobline.payments.domain.PaymentId;

import java.util.concurrent.CompletionStage;

import io.wobline.payments.domain.PaymentStatus;
import org.apache.pekko.Done;

public interface PaymentViewRepository {

    CompletionStage<Done> save(
            String timestamp,
            PaymentId paymentId,
            String cardNumber,
            String expiryDate,
            String cvv,
            Double amount,
            String currency,
            String merchantId,
            PaymentStatus status);
}
