package tech.parasol.akka.workshop.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JavaProfileActor extends UntypedAbstractActor {

    Logger logger = LoggerFactory.getLogger(this.getClass().getName());


    static public Props props() {
        return Props.create(JavaProfileActor.class, JavaProfileActor::new);
    }


    @Override
    public void preStart() {
        System.out.println("ProfileActor preStart uid = " + getSelf().path().uid() + ", path = " + getSelf().path() + ", object hash = " + this.hashCode());
    }

    @Override
    public void postStop() {
        System.out.println("ProfileActor stopped uid = " + getSelf().path().uid() + ", path = " + getSelf().path() + ", object hash = " + this.hashCode());
    }


    @Override
    public void postRestart(Throwable reason) throws Exception {
        System.out.println("ProfileActor postRestart uid = " + getSelf().path().uid() + ", path = " + getSelf().path() + ", object hash = " + this.hashCode());
    }

    @Override
    public void onReceive(Object message) throws Exception {

        if(message instanceof String) {
            getSender().tell("Greeting Complete", ActorRef.noSender());
            return;
        } else if(message instanceof tech.parasol.akka.workshop.cluster.User) {
            tech.parasol.akka.workshop.cluster.User user = (tech.parasol.akka.workshop.cluster.User)message;
            logger.info("Add user: userId = {}, userName = ${}", user.getUserId(), user.getUserName());
            sender().tell(user, ActorRef.noSender());
            return;
        } else {
            unhandled(message);
        }
    }


}
