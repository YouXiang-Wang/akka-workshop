package tech.parasol.akka.workshop.actor;



import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import akka.japi.Option;
import akka.japi.pf.ReceiveBuilder;
import scala.PartialFunction;

import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;
import static akka.pattern.Patterns.gracefulStop;

public class GreeterActor extends AbstractActor {

    private String name;

    public static class Greeting implements Serializable {
        public final String who;
        public Greeting(String who) { this.who = who; }
    }

    public static class ForwardGreeting implements Serializable {
        public final String who;
        public ForwardGreeting(String who) { this.who = who; }
    }


    LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    static public Props props() {
        return Props.create(GreeterActor.class, "UNKNOWN");
    }


    static public Props props(String name) {
        return Props.create(GreeterActor.class, name);
    }

    public GreeterActor(String name) {
        System.out.println("name ===> " + name);
        this.name = name;
    }

    public static enum Msg {
        WORKING, EXCEPTION, STOP, RESTART, RESUME, BACK, SLEEP
    }


    /*

    private static SupervisorStrategy strategy = new AllForOneStrategy(-1,
            Duration.ofSeconds(10000), new Function<Throwable, SupervisorStrategy.Directive>() {
        @Override
        public SupervisorStrategy.Directive apply(Throwable t) {
            if (t instanceof Exception) {
                return restart();
            } else {
                return escalate();
            }
        }
    });


    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

     */

    @Override
    public void preStart() {
        logger.info("GreeterActor preStart uid = " + getSelf().path().uid() + ", path = " + getSelf().path() + ", object hash = " + this.hashCode());
    }

    @Override
    public void postStop() {
        logger.info("GreeterActor stopped uid = " + getSelf().path().uid() + ", path = " + getSelf().path() + ", object hash = " + this.hashCode());
    }

    @Override
    public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
        logger.info("GreeterActor preRestart uid = " + getSelf().path().uid() + ", path = " + getSelf().path() + ", object hash = " + this.hashCode());
        super.preRestart(reason, message);
    }

    @Override
    public void postRestart(Throwable reason) throws Exception {
        logger.info("GreeterActor postRestart uid = " + getSelf().path().uid() + ", path = " + getSelf().path() + ", object hash = " + this.hashCode());
    }




    public Receive createReceive() {
        return receiveBuilder()
                .match(Msg.class, s -> s == Msg.WORKING, s -> {
                    logger.info("I am working");
                })
                .match(Msg.class, s -> s == Msg.STOP, s -> {
                    logger.info("I am stopped");
                })
                .match(Msg.class, s -> s == Msg.SLEEP, s -> {
                    logger.info("I am going to sleep");
                    Thread.sleep(3000);
                    getSender().tell("I am awake", getSelf());
                })
                .match(Greeting.class, s -> {
                    System.out.println("Hello " + s.who);
                    ForwardGreeting greeting = new ForwardGreeting(s.who);
                    ActorRef ref = context().actorOf(Props.create(ForwardGreeterActor.class), System.currentTimeMillis() + "");
                    ref.forward(greeting, getContext());
                    ref.tell(greeting, self());
                    context().watch(ref);
                    ref.tell(akka.actor.Kill.getInstance(), ActorRef.noSender());

                })
                .match(String.class, s -> s.startsWith("Kill"), s -> {
                    logger.info("Stopping the actor");
                    context().stop(self());
                })
                .match(String.class, s -> s.startsWith("Greeting"), s -> {
                    //logger.info("Received String message start with: {}", s);
                    getSender().tell("Greeting Complete", self());
                })
                .match(String.class, s -> {
                    //logger.info("Received String message start with: {}", s);
                    /**
                     *
                     */

                    getSender().tell("Greeting Complete", self());
                })
                .match(Terminated.class, s -> {
                    logger.info("Watch terminated. actor = {}", s.actor());
                    context().unwatch(s.actor());
                    /**
                     * do something here
                     */
                })
                .matchAny(o -> logger.info("received unknown message: {}", o.toString()))
                .build();
    }


    public void onReceive(Object message) throws Exception {

        if(message == Msg.WORKING) {
            logger.info("I am  working");
        } else if(message == Msg.STOP) {
            logger.info("I am stopped");
            getContext().stop(getSelf());

        } else if(message == Msg.SLEEP) {
            logger.info("I am going to sleep");
            Thread.sleep(3000);
            getSender().tell("I am awake", getSelf());

        } else if(message instanceof Greeting) {
            System.out.println("Hello " + ((Greeting) message).who);
            ForwardGreeting greeting = new ForwardGreeting(((Greeting) message).who);
            ActorRef ref = context().actorOf(Props.create(ForwardGreeterActor.class), System.currentTimeMillis() + "");
            ref.forward(greeting, getContext());
        } else if(message instanceof String) {
            getSender().tell("Greeting Complete", ActorRef.noSender());
            return;
        } else {
            unhandled(message);
        }
    }


}
