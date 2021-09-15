package tech.parasol.akka.workshop.actor;


import akka.actor.AbstractActor;
import akka.actor.Props;

class SupervisedActor extends AbstractActor {
    static Props props() {
        return Props.create(SupervisedActor.class, SupervisedActor::new);
    }

    @Override
    public void preStart() {
        System.out.println("supervised actor started");
    }

    @Override
    public void postStop() {
        System.out.println("supervised actor stopped");
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .matchEquals(
                        "fail",
                        f -> {
                            System.out.println("supervised actor fails now");
                            throw new Exception("I failed!");
                        })
                .build();
    }
}