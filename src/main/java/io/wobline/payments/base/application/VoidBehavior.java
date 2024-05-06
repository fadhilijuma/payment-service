package io.wobline.payments.base.application;


import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

public class VoidBehavior {

    public static Behavior<Void> create() {
        return Behaviors.receive(Void.class).build();
    }
}
