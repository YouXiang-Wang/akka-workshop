package tech.parasol.akka.workshop.stream;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import tech.parasol.akka.workshop.actor.GreeterActor;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static akka.pattern.Patterns.ask;

public class AkkStream {


    public static ActorSystem system = ActorSystem.create("AkkStream");
    static final Duration t = Duration.ofSeconds(5);



    public static void main(String[] args) {
        system.registerOnTermination(() -> System.out.println("Termination"));

        final Source<Integer, NotUsed> source =
                Source.from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        // note that the Future is scala.concurrent.Future
        final Sink<Integer, CompletionStage<Integer>> sink =
                Sink.<Integer, Integer>fold(0, (aggr, next) -> aggr + next);

        // connect the Source to the Sink, obtaining a RunnableFlow
        final RunnableGraph<CompletionStage<Integer>> runnable = source.toMat(sink, Keep.right());

        // materialize the flow
        final CompletionStage<Integer> sum = runnable.run(system);



        /*
        sum.thenApply(() -> {

        })

         */


    }
}


