package io.wobline.payments.application.projection;

import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.wobline.payments.domain.PaymentEvent;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.apache.pekko.Done;
import org.apache.pekko.actor.CoordinatedShutdown;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.SpawnProtocol;
import org.apache.pekko.actor.typed.javadsl.AskPattern;
import org.apache.pekko.cluster.typed.ClusterSingleton;
import org.apache.pekko.cluster.typed.SingletonActor;
import org.apache.pekko.projection.eventsourced.EventEnvelope;
import org.apache.pekko.projection.Projection;
import org.apache.pekko.projection.ProjectionBehavior;
import org.apache.pekko.projection.ProjectionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectionLauncher {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ActorSystem<SpawnProtocol.Command> system;
    private final Duration timeout = Duration.ofSeconds(3);
    private List<Projection<EventEnvelope<PaymentEvent>>> singletonProjections = List.empty();
    private List<Projection<EventEnvelope<PaymentEvent>>> localProjections = List.empty();
    private List<Tuple2<ProjectionId, ActorRef<ProjectionBehavior.Command>>> localProjectionRefs = List.empty();

    public ProjectionLauncher(ActorSystem<SpawnProtocol.Command> system) {
        this.system = system;
    }

    @SafeVarargs
    public final ProjectionLauncher withSingletonProjections(Projection<EventEnvelope<PaymentEvent>>... singletonProjections) {
        this.singletonProjections = List.of(singletonProjections);
        return this;
    }

    @SafeVarargs
    public final ProjectionLauncher withLocalProjections(Projection<EventEnvelope<PaymentEvent>>... localProjections) {
        this.localProjections = List.of(localProjections);
        return this;
    }

    public void runProjections() {
        runSingletonProjections();
        runLocalProjections();
        CoordinatedShutdown.get(system).addTask(CoordinatedShutdown.PhaseBeforeActorSystemTerminate(),
                "shutdown projections", () -> {
                    log.info("projection shutting down started");
                    shutdownProjections();
                    log.info("projection shutting down finished");
                    return CompletableFuture.completedFuture(Done.getInstance());
                });
    }

    private void runLocalProjections() {
        localProjectionRefs = localProjections.map(projection -> {
            ProjectionId projectionId = projection.projectionId();
            log.info("Starting local projection {}", projectionId);
            CompletionStage<ActorRef<ProjectionBehavior.Command>> result = AskPattern.ask(system,
                    r -> new SpawnProtocol.Spawn<>(ProjectionBehavior.create(projection),
                            projectionId.id(), Props.empty(), r), timeout, system.scheduler());

            try {
                return result.thenApply(ref -> {
                    log.debug("Projection: {} launched", projectionId);
                    return new Tuple2<>(projectionId, ref);
                }).toCompletableFuture().get();
            } catch (ExecutionException e) {
                throw new IllegalStateException("Error while creating projection " + projectionId, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Error while creating projection " + projectionId, e);
            }
        });
    }

    private void runSingletonProjections() {
        singletonProjections.forEach(projection -> {
            log.info("Starting singleton projection " + projection.projectionId());
            ClusterSingleton.get(system)
                    .init(SingletonActor.of(ProjectionBehavior.create(projection), projection.projectionId().id()));
        });
    }

    public void shutdownProjections() {
        log.info("Shutting down {} local projections", localProjections.size());
        localProjectionRefs.forEach(tuple -> {
            log.info("Shutting down projection {}", tuple._1);
            tuple._2.tell(ProjectionBehavior.stopMessage());
        });
    }
}
