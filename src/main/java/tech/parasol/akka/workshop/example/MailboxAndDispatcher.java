package tech.parasol.akka.workshop.example;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import tech.parasol.akka.workshop.actor.GreeterActor;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static akka.pattern.Patterns.ask;

public class MailboxAndDispatcher {

    public static ActorSystem system = ActorSystem.create("MailboxAndDispatcher");
    static final Duration t = Duration.ofSeconds(5);

    public static ActorRef createActor() {
        final ActorRef greeterActor =
                system.actorOf(GreeterActor.props().withDispatcher("custom-dispatcher"), "greeterActor");
        return greeterActor;
    }

    public static void tellMessage(ActorRef actorRef, Object message) {
        actorRef.tell(message, ActorRef.noSender());
    }

    public static void tellMessage(ActorSelection actorSelection, Object message) {
        actorSelection.tell(message, ActorRef.noSender());
    }

    public static void askMessage(ActorRef actorRef, Object message) {
        CompletableFuture<Object> future2 = ask(actorRef, message, t).toCompletableFuture();
        try {
            String res = (String)future2.get();
            System.out.println("Result ===> " + res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ActorSelection lookupActor(String actorPath) {
        final ActorSelection selection = system.actorSelection(actorPath);
        return selection;
    }



    public static void main(String[] args) {

        final ActorRef greeterActor = createActor();
        tellMessage(greeterActor, GreeterActor.Msg.WORKING);
        askMessage(greeterActor, "Greeting");

        tellMessage(greeterActor, new GreeterActor.Greeting("wang"));

        final ActorSelection selection = system.actorSelection("/user/greeterActor");
        tellMessage(selection, GreeterActor.Msg.WORKING);

    }
}
