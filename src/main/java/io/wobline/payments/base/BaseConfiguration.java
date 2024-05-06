package io.wobline.payments.base;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.wobline.payments.base.application.SpawningBehavior;
import io.wobline.payments.base.domain.Clock;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.SpawnProtocol;
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BaseConfiguration {

    @Bean
    public Config config() {
        return ConfigFactory.load();
    }

    @Bean(destroyMethod = "terminate")
    public ActorSystem<SpawnProtocol.Command> actorSystem(Config config) {
        return ActorSystem.create(SpawningBehavior.create(), "payments-processor", config);
    }

    @Bean
    public ClusterSharding clusterSharding(ActorSystem<?> actorSystem) {
        return ClusterSharding.get(actorSystem);
    }

    @Bean
    Clock clock() {
        return new Clock.UtcClock();
    }
}
