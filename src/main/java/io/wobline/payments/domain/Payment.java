package io.wobline.payments.domain;

import io.vavr.collection.List;
import io.vavr.control.Either;
import io.wobline.payments.base.domain.Clock;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

public record Payment(
        String timestamp,
        PaymentId paymentId,
        String cardNumber,
        String expiryDate,
        String cvv,
        Double amount,
        String currency,
        String merchantId,
        PaymentStatus status)
        implements Serializable {

    public static Payment create(PaymentEvent.PaymentProcessed processed, PaymentStatus status) {
        return new Payment(
                processed
                        .createdAt()
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")),
                processed.paymentId(),
                processed.cardNumber(),
                processed.expiryDate(),
                processed.cvv(),
                processed.amount(),
                processed.currency(),
                processed.merchantId(),
                status);
    }

    public Either<PaymentCommandError, List<PaymentEvent>> process(
            PaymentCommand command, Clock clock) {
        return switch (command) {
            case PaymentCommand.ProcessPayment ignored -> left(PaymentCommandError.PAYMENT_ALREADY_EXISTS);
            case PaymentCommand.UpdatePaymentStatus paymentStatus -> handleUpdate(paymentStatus, clock);
        };
    }

    private Either<PaymentCommandError, List<PaymentEvent>> handleUpdate(
            PaymentCommand.UpdatePaymentStatus command, Clock clock) {
        boolean isOddLastDigit = Integer.parseInt(command.cardNumber().substring(command.cardNumber().length() - 1)) % 2 != 0;
        boolean isApproved = !isOddLastDigit;

        if (isApproved) {
            return right(List.of(new PaymentEvent.PaymentStatusUpdated(command.paymentId(), clock.now(), command.cardNumber(), command.expiryDate(), command.cvv(), command.amount(), command.currency(), command.merchantId(), PaymentStatus.APPROVED)));

        } else {
            return right(List.of(new PaymentEvent.PaymentStatusUpdated(command.paymentId(), clock.now(), command.cardNumber(), command.expiryDate(), command.cvv(), command.amount(), command.currency(), command.merchantId(), PaymentStatus.DECLINED)));

        }
    }

    public Payment apply(PaymentEvent event) {
        return switch (event) {
            case PaymentEvent.PaymentProcessed processed -> applyProcessed(processed);
            case PaymentEvent.PaymentStatusUpdated updated -> applyUpdated(updated);
        };
    }

    private Payment applyProcessed(PaymentEvent.PaymentProcessed processed) {
        return new Payment(
                processed
                        .createdAt()
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")),
                processed.paymentId(),
                processed.cardNumber(),
                processed.expiryDate(),
                processed.cvv(),
                processed.amount(),
                processed.currency(),
                processed.merchantId(),
                PaymentStatus.PENDING);
    }

    private Payment applyUpdated(PaymentEvent.PaymentStatusUpdated updated) {
        return new Payment(
                updated
                        .createdAt()
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")),
                updated.paymentId(),
                updated.cardNumber(),
                updated.expiryDate(),
                updated.cvv(),
                updated.amount(),
                updated.currency(),
                updated.merchantId(),
                updated.status());
    }
}
