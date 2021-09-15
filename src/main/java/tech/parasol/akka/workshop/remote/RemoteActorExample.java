package tech.parasol.akka.workshop.remote;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import tech.parasol.akka.workshop.actor.GreeterActor;

public class RemoteActorExample {

    public static String actorSystemName = "RemoteActorExample";
    public static ActorSystem system = ActorSystem.create(actorSystemName);

    public static ActorRef createActor(String name) {
        final ActorRef greeterActor = system.actorOf(GreeterActor.props(), name);
        return greeterActor;
    }

    public static void tellMessage(ActorSelection actorSelection, Object message) {
        actorSelection.tell(message, ActorRef.noSender());
    }

    public static void main(String[] args) {
        createActor("greeterActor");
        String ip = "127.0.0.1";
        int port = 25520;
        final ActorSelection selection = system.actorSelection("akka://" + actorSystemName + "@" + ip + ":" + port + "/user/greeterActor");
        tellMessage(selection, GreeterActor.Msg.WORKING);
    }
}
