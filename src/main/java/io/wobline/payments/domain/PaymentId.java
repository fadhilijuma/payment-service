package io.wobline.payments.domain;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.io.Serializable;
import java.util.UUID;

public record PaymentId(UUID id) implements Serializable {

  public static PaymentId of(String merchantId, String cardNumber, double amount, String currency) {
    String combinedData = merchantId + cardNumber + amount + currency;
    String paymentId = hash(combinedData);
    UUID paymentUUID = UUID.nameUUIDFromBytes(paymentId.getBytes());
    return new PaymentId(paymentUUID);
  }

  public static PaymentId of(UUID id) {
    return new PaymentId(id);
  }

  private static String hash(String data) {
    return BCrypt.withDefaults().hashToString(12, data.toCharArray());
  }

  private static boolean verify(String data, String hash) {
    return BCrypt.verifyer().verify(data.toCharArray(), hash).verified;
  }
}
