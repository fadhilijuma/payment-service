package io.wobline.payments.application.projection;


import static java.time.Duration.ofSeconds;
import static org.apache.pekko.projection.HandlerRecoveryStrategy.retryAndFail;

import io.wobline.payments.domain.PaymentEvent;
import java.time.Duration;
import javax.sql.DataSource;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.persistence.query.Offset;
import org.apache.pekko.projection.eventsourced.EventEnvelope;
import org.apache.pekko.projection.Projection;
import org.apache.pekko.projection.ProjectionId;
import org.apache.pekko.projection.javadsl.SourceProvider;
import org.apache.pekko.projection.jdbc.javadsl.JdbcProjection;

public class PaymentViewProjection {

    public static final ProjectionId PROJECTION_ID = ProjectionId.of("payment-events", "payment-view");

    private final ActorSystem<?> actorSystem;
    private final DataSource dataSource;
    private final PaymentViewEventHandler paymentViewEventHandler;
    private final int saveOffsetAfterEnvelopes = 100;
    private final Duration saveOffsetAfterDuration = Duration.ofMillis(500);

    public PaymentViewProjection(ActorSystem<?> actorSystem, DataSource dataSource, PaymentViewEventHandler paymentViewEventHandler) {
        this.actorSystem = actorSystem;
        this.dataSource = dataSource;
        this.paymentViewEventHandler = paymentViewEventHandler;
    }

    public Projection<EventEnvelope<PaymentEvent>> create(SourceProvider<Offset, EventEnvelope<PaymentEvent>> sourceProvider) {
        return JdbcProjection.atLeastOnceAsync(
                        PROJECTION_ID,
                        sourceProvider,
                        () -> new DataSourceJdbcSession(dataSource),
                        () -> paymentViewEventHandler,
                        actorSystem)
                .withSaveOffset(saveOffsetAfterEnvelopes, saveOffsetAfterDuration)
                .withRecoveryStrategy(retryAndFail(4, ofSeconds(5))) //could be configured in application.conf
                .withRestartBackoff(ofSeconds(3), ofSeconds(30), 0.1d); //could be configured in application.conf
    }


}
