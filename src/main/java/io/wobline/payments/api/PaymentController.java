package io.wobline.payments.api;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.ResponseEntity.*;

import io.wobline.payments.application.PaymentEntityResponse;
import io.wobline.payments.application.PaymentService;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import io.wobline.payments.domain.PaymentId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "v1/payments")
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping
  public Mono<ResponseEntity<String>> create(@RequestBody ProcessPaymentRequest request) {
    CompletionStage<ResponseEntity<String>> paymentResponse =
        paymentService
            .processPayment(
                request.cardNumber(),
                request.expiryDate(),
                request.cvv(),
                request.amount(),
                request.currency(),
                request.merchantId())
            .thenApply(
                result ->
                    switch (result) {
                      case PaymentEntityResponse.CommandProcessed ignored ->
                          new ResponseEntity<>("Payment Processed", CREATED);
                      case PaymentEntityResponse.CommandRejected rejected ->
                          transformRejection(rejected);
                    });

    return Mono.fromCompletionStage(paymentResponse);
  }

  @GetMapping(value = "{paymentId}", produces = "application/json")
  public Mono<ResponseEntity<PaymentResponse>> fetchById(@PathVariable UUID paymentId) {
    CompletionStage<ResponseEntity<PaymentResponse>> response =
        paymentService
            .fetch(PaymentId.of(paymentId))
            .thenApply(
                result ->
                    result
                        .map(PaymentResponse::from)
                        .map(ok()::body)
                        .getOrElse(notFound().build()));
    return Mono.fromCompletionStage(response);
  }

  private ResponseEntity<String> transformRejection(
      PaymentEntityResponse.CommandRejected rejected) {
    return switch (rejected.error()) {
      case PAYMENT_ALREADY_EXISTS -> new ResponseEntity<>("Payment already created", CONFLICT);
      default -> badRequest().body("Request failed with: " + rejected.error().name());
    };
  }
}
