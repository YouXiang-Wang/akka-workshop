package tech.parasol.akka.workshop.part1.actor;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;


public class ForwardGreeterActor extends UntypedAbstractActor {

    LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    @Override
    public void preStart() {
        System.out.println("GreeterActor preStart uid = " + getSelf().path().uid() + ", path = " + getSelf().path() + ", object hash = " + this.hashCode());
    }


    @Override
    public void onReceive(Object message) throws Exception {
        if(message instanceof GreeterActor.ForwardGreeting) {
            System.out.println("Hello " + ((GreeterActor.ForwardGreeting) message).who);
            System.out.println("sender ===> " + getSender());
        } else {
            System.out.println("unhandled ===> " + message);
            unhandled(message);
        }
    }

}
