package io.wobline.payments.domain;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import io.vavr.control.Either;
import io.wobline.payments.base.domain.Clock;
import java.time.LocalDate;

public class PaymentProcessor {
  public static Either<PaymentCommandError, PaymentEvent.PaymentProcessed> process(
      PaymentCommand.ProcessPayment command, Clock clock) {
    if (!isValid(command.cardNumber(), command.expiryDate(), command.amount())) {
      return left(PaymentCommandError.INVALID_PAYMENT_REQUEST);
    }
    var paymentCreated =
        new PaymentEvent.PaymentProcessed(
            command.paymentId(),
            clock.now(),
            command.cardNumber(),
            command.expiryDate(),
            command.cvv(),
            command.amount(),
            command.currency(),
            command.merchantId(),
            PaymentStatus.PENDING);
    return right(paymentCreated);
  }

  private static boolean isValid(String cardNumber, String expiryDate, Double amount) {
    return isCardNumberValid(cardNumber) && expiryDateIsValid(expiryDate) && isAmountValid(amount);
  }

  private static boolean isCardNumberValid(String cardNumber) {
    boolean isValid = luhnAlgorithm(cardNumber);
    return luhnAlgorithm(cardNumber);
  }

  private static boolean expiryDateIsValid(String expiryDate) {
    if (!expiryDate.matches("\\d{2}/\\d{2}")) {
      return false;
    }

    String[] parts = expiryDate.split("/");
    int month = Integer.parseInt(parts[0]);
    int year = Integer.parseInt(parts[1]);

    LocalDate currentDate = LocalDate.now();

    int currentYear = currentDate.getYear() % 100;
    int currentMonth = currentDate.getMonthValue();

    if (year < currentYear || (year == currentYear && month < currentMonth)) {
      return false;
    }

    return month >= 1 && month <= 12;
  }

  private static boolean isAmountValid(double amount) {
    return amount > 0;
  }

  public static boolean luhnAlgorithm(String cardNumber) {

    if (cardNumber == null || cardNumber.isEmpty()) {
      return false;
    }

    int sum = 0;
    boolean isSecondDigit = false;

    for (int i = cardNumber.length() - 1; i >= 0; i--) {
      int digit = Character.getNumericValue(cardNumber.charAt(i));

      if (isSecondDigit) {
        digit *= 2;
        if (digit > 9) {
          digit = digit - 10 + 1;
        }
      }
      sum += digit;
      isSecondDigit = !isSecondDigit;
    }

    return (sum % 10 == 0);
  }
}
