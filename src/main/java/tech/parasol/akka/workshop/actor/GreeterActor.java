package tech.parasol.akka.workshop.part1.actor;



import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.io.Serializable;


public class GreeterActor extends UntypedAbstractActor {

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
        return Props.create(GreeterActor.class, GreeterActor::new);
    }

    public static enum Msg {
        WORKING, EXCEPTION, STOP, RESTART, RESUME, BACK, SLEEP
    }

    @Override
    public void preStart() {
        System.out.println("GreeterActor preStart uid = " + getSelf().path().uid() + ", path = " + getSelf().path() + ", object hash = " + this.hashCode());
    }

    @Override
    public void postStop() {
        System.out.println("GreeterActor stopped uid = " + getSelf().path().uid() + ", path = " + getSelf().path() + ", object hash = " + this.hashCode());
    }


    @Override
    public void postRestart(Throwable reason) throws Exception {
        System.out.println("GreeterActor postRestart uid = " + getSelf().path().uid() + ", path = " + getSelf().path() + ", object hash = " + this.hashCode());
    }

    @Override
    public void onReceive(Object message) throws Exception {

        if(message == Msg.WORKING) {
            logger.info("I am  working");
        } else if(message == Msg.EXCEPTION) {
            throw new Exception("I failed!");

        } else if(message == Msg.RESTART){
            logger.info("I will be restarted");
            throw new NullPointerException();

        } else if(message == Msg.RESUME) {
            logger.info("I will be resume");
            throw new ArithmeticException();

        } else if(message == Msg.STOP) {
            logger.info("I am stopped");
            getContext().stop(getSelf());

        } else if(message == Msg.BACK) {
            getSender().tell("I am alive", getSelf());

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
            //getContext().actorSelection("../sysout").tell("Hello " + msg, getSelf());
            //getSender().tell("Greeting Complete", getSelf());
            getSender().tell("Greeting Complete", ActorRef.noSender());
            return;
        } else {
            unhandled(message);
        }
    }


}
