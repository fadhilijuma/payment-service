package io.wobline.payments.base.application;


import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.SpawnProtocol;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

public abstract class SpawningBehavior {
    private SpawningBehavior() {
    }

    public static Behavior<SpawnProtocol.Command> create() {
        return Behaviors.setup(context -> SpawnProtocol.create());
    }
}
