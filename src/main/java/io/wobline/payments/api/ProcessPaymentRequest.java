package io.wobline.payments.api;

public record ProcessPaymentRequest(String cardNumber, String expiryDate,
                                    String cvv, Double amount, String currency,
                                    String merchantId) {
}
