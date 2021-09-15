package tech.parasol.akka.workshop.example;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Kill;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.AskTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.parasol.akka.workshop.actor.GreeterActor;
import static akka.pattern.Patterns.ask;
import static akka.pattern.Patterns.pipe;
import static akka.pattern.Patterns.gracefulStop;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class HelloAkka {

    public static ActorSystem system = ActorSystem.create("HelloAkka");
    public static Logger logger = LoggerFactory.getLogger(HelloAkka.class.getName());
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
            System.out.println("AskResult ===> " + res);
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
            System.out.println("AskResult ===> " + res);
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

        ActorRef greeterActor = createActor();

        logger.info(" === tellMessage === ");
        tellMessage(greeterActor, GreeterActor.Msg.WORKING);

        logger.info("=== askMessage ===");
        askMessage(greeterActor, "Greeting");
        logger.info("=== askMessage2 ===");
        askMessage2(greeterActor, "another request");

        tellMessage(greeterActor, new GreeterActor.Greeting("wang"));
        logger.info("=== actorSelection ===");
        final ActorSelection selection = system.actorSelection("/user/greeterActor");
        tellMessage(selection, GreeterActor.Msg.WORKING);


        /*
        try {
            greeterActor.tell("Kill", ActorRef.noSender());
            Thread.sleep(100);
            greeterActor = createActor();
            greeterActor.tell(akka.actor.Kill.getInstance(), ActorRef.noSender());
            Thread.sleep(100);
            greeterActor = createActor();
            greeterActor.tell(akka.actor.PoisonPill.getInstance(), ActorRef.noSender());

        } catch (Exception e) {
            e.printStackTrace();
        }

         */
    }
}
