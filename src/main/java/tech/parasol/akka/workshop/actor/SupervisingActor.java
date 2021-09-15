package tech.parasol.akka.workshop.actor;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class SupervisingActor extends AbstractActor {
    public static Props props() {
        return Props.create(SupervisingActor.class, SupervisingActor::new);
    }

    ActorRef child = getContext().actorOf(SupervisedActor.props(), "supervised-actor");

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals(
                        "failChild",
                        f -> {
                            child.tell("fail", getSelf());
                        })
                .build();
    }
}
