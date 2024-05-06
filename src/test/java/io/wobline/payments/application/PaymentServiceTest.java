package io.wobline.payments.application;

import static io.wobline.payments.application.Blocking.await;
import static org.assertj.core.api.Assertions.assertThat;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import io.wobline.payments.base.domain.Clock;
import io.wobline.payments.domain.PaymentId;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Adapter;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.persistence.testkit.PersistenceTestKitPlugin;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

    private static Config config =
            PersistenceTestKitPlugin.config().withFallback(ConfigFactory.load());
    private static ActorSystem system = ActorSystem.create("payments-processor", config);
    private ClusterSharding sharding = ClusterSharding.get(Adapter.toTyped(system));
    private Clock clock = new Clock.UtcClock();
    private PaymentService paymentService = new PaymentService(sharding, clock);

    @AfterAll
    public static void cleanUp() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    public void shouldProcessPayment() throws ExecutionException, InterruptedException {
        // when
        var result =
                await(
                        paymentService.processPayment(
                                "4987050011059239", "12/25", "123", 200.0, "USD", "merchant13"));

        // then
        assertThat(result).isInstanceOf(PaymentEntityResponse.CommandProcessed.class);
    }

    @Test
    public void shouldReturnEmptyPayment() throws ExecutionException, InterruptedException {
        // given
        var paymentId = PaymentId.of(UUID.nameUUIDFromBytes("test".getBytes()));

        // when
        var payment = await(paymentService.fetch(paymentId));

        // then
        assertThat(payment.isDefined()).isFalse();
    }
}
