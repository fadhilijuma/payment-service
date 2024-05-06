package io.wobline.payments;

import com.zaxxer.hikari.HikariDataSource;
import io.wobline.payments.application.PaymentEntity;
import io.wobline.payments.application.PaymentService;
import io.wobline.payments.application.projection.PaymentViewEventHandler;
import io.wobline.payments.application.projection.PaymentViewProjection;
import io.wobline.payments.application.projection.PaymentViewRepository;
import io.wobline.payments.application.projection.ProjectionLauncher;
import io.wobline.payments.base.domain.Clock;
import io.wobline.payments.domain.PaymentEvent;
import io.wobline.payments.infrastructure.InMemoryPaymentViewRepository;

import javax.sql.DataSource;

import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.SpawnProtocol;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.apache.pekko.persistence.jdbc.query.javadsl.JdbcReadJournal;
import org.apache.pekko.persistence.query.Offset;
import org.apache.pekko.projection.eventsourced.EventEnvelope;
import org.apache.pekko.projection.javadsl.SourceProvider;
import org.apache.pekko.projection.eventsourced.javadsl.EventSourcedProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentConfiguration {

    private final ActorSystem<SpawnProtocol.Command> system;
    private final ClusterSharding sharding;

    public PaymentConfiguration(ActorSystem<SpawnProtocol.Command> system, ClusterSharding sharding, Clock clock) {
        this.system = system;
        this.sharding = sharding;
        this.clock = clock;
    }

    private final Clock clock;


    @Bean
    public PaymentService paymentService() {
        return new PaymentService(sharding, clock);
    }

    @Bean
    public PaymentViewRepository paymentViewRepository() {
        return new InMemoryPaymentViewRepository();
    }

    @Bean(initMethod = "runProjections")
    public ProjectionLauncher projectionLauncher(PaymentViewRepository paymentViewRepository) {
        PaymentViewEventHandler paymentViewEventHandler = new PaymentViewEventHandler(paymentViewRepository);
        SourceProvider<Offset, EventEnvelope<PaymentEvent>> sourceProvider = EventSourcedProvider.eventsByTag(system, JdbcReadJournal.Identifier(), PaymentEntity.PAYMENT_EVENT_TAG);
        PaymentViewProjection paymentViewProjection = new PaymentViewProjection(system, dataSource(), paymentViewEventHandler);
        ProjectionLauncher projectionLauncher = new ProjectionLauncher(system);
        projectionLauncher.withLocalProjections(paymentViewProjection.create(sourceProvider));
        return projectionLauncher;
    }

    public DataSource dataSource() {
        var hikariDataSource = new HikariDataSource();
        hikariDataSource.setPoolName("projection-data-source");
        hikariDataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres");
        hikariDataSource.setUsername("admin");
        hikariDataSource.setPassword("admin");
        // https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
        hikariDataSource.setMaximumPoolSize(5);
        hikariDataSource.setRegisterMbeans(true);
        return hikariDataSource;
    }
}
