package io.wobline.payments.api;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.reactive.server.WebTestClient;

/** Run `docker-compose -f docker-compose-jdbc.yml up` in `development` folder */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class PaymentControllerItTest {

  @Autowired private WebTestClient webClient;

  @Test
  public void shouldProcessPayment() {
    // given
    var processPaymentRequest =
        new ProcessPaymentRequest("4987050011059239", "12/25", "123", 200.0, "USD", "merchant13");

    // when //then
    processPayment(processPaymentRequest);
  }

  @Test
  public void shouldGetPaymentById() {
    // given
    var processPaymentRequest =
        new ProcessPaymentRequest("4987050011059239", "12/25", "123", 200.0, "USD", "merchant13");

    processPayment(processPaymentRequest);

    // when //then
    webClient
        .get()
        .uri("/v1/payments/{paymentId}", "25cdb94c-badb-3e38-b9ff-6c3617c54271")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(PaymentResponse.class)
        .value(shouldHaveId("25cdb94c-badb-3e38-b9ff-6c3617c54271"));
  }

  private BaseMatcher<PaymentResponse> shouldHaveId(String paymentId) {
    return new BaseMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendValue("PaymentResponse should with id: " + paymentId);
      }

      @Override
      public boolean matches(Object o) {
        if (o instanceof PaymentResponse paymentResponse) {
          return paymentResponse.id().toString().equals(paymentId);
        } else {
          return false;
        }
      }
    };
  }

  private void processPayment(ProcessPaymentRequest processPaymentRequest) {
    webClient
        .post()
        .uri("/v1/payments")
        .bodyValue(processPaymentRequest)
        .exchange()
        .expectStatus()
        .isCreated();
  }
}
