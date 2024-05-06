package io.wobline.payments.infrastructure;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.wobline.payments.application.projection.PaymentView;
import io.wobline.payments.application.projection.PaymentViewRepository;
import io.wobline.payments.domain.PaymentId;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.wobline.payments.domain.PaymentStatus;
import org.apache.pekko.Done;

public class InMemoryPaymentViewRepository implements PaymentViewRepository {

  private ConcurrentMap<PaymentId, PaymentView> store = new ConcurrentHashMap<>();

  @Override
  public CompletionStage<Done> save(
      String timestamp,
      PaymentId paymentId,
      String cardNumber,
      String expiryDate,
      String cvv,
      Double amount,
      String currency,
      String merchantId,
      PaymentStatus status) {
    return supplyAsync(
        () -> {
          store.put(
              paymentId,
              new PaymentView(
                  paymentId.toString(),
                  cardNumber,
                  expiryDate,
                  cvv,
                  amount,
                  currency,
                  merchantId,
                  status));
          return Done.done();
        });
  }
}
