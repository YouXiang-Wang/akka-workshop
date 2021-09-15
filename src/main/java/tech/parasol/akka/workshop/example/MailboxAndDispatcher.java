package tech.parasol.akka.workshop.example;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import tech.parasol.akka.workshop.actor.GreeterActor;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static akka.pattern.Patterns.ask;

public class MailboxAndDispatcher {

    public static ActorSystem system = ActorSystem.create("HelloAkka");
    static final Duration t = Duration.ofSeconds(5);

    public static ActorRef createActor() {
        final ActorRef greeterActor =
                system.actorOf(GreeterActor.props(), "greeterActor");
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


    public static void askMessage2(ActorRef actorRef, Object message) {
        CompletableFuture<Object> future2 = ask(actorRef, message, t).toCompletableFuture();
        CompletableFuture<Object> future1 = ask(actorRef, message, t).toCompletableFuture();
        CompletableFuture<String> transformed =
                CompletableFuture.allOf(future1, future2)
                        .thenApply(
                                v -> {
                                    String x = (String) future1.join();
                                    String s = (String) future2.join();
                                    return x + ":" +  s;
                                });

        try {
            String res = transformed.get();
            System.out.println("Result ===> " + res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        /**
         * Termination hook
         */

        system.registerOnTermination(() -> System.out.println("Termination"));

        /**
         *
         * ActorRef supervisingActor = system.actorOf(SupervisingActor.props(), "supervising-actor");
         * supervisingActor.tell("failChild", ActorRef.noSender());
         *
         */

        final ActorRef greeterActor = createActor();

        tellMessage(greeterActor, GreeterActor.Msg.WORKING);
        askMessage(greeterActor, "Greeting");
        askMessage2(greeterActor, "another request");

        tellMessage(greeterActor, new GreeterActor.Greeting("wang"));


        final ActorSelection selection = system.actorSelection("/user/greeterActor");
        tellMessage(selection, GreeterActor.Msg.WORKING);

    }
}
