package tech.parasol.akka.workshop.part1;

import akka.actor.ActorSystem;

public class HelloAkka {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("HelloAkka");
        System.out.println("Hello Akka");
    }
}
