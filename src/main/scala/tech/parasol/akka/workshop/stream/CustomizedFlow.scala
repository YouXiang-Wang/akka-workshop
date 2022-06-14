package tech.parasol.akka.workshop.stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import tech.parasol.akka.workshop.stream.ParallelismStream.{parallelism, spin, test1}

import scala.concurrent.{ExecutionContextExecutor, Future}

object CustomizedFlow {

  implicit val system = ActorSystem("CustomizedFlow")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher


  def customized1 = {

    Source(1 to 10)
      //.map(spin)
      .via(new HoldWithWait)
      .map(println(_))
      .runWith(Sink.ignore)


  }

  def main(args: Array[String]): Unit = {

    customized1
  }

}
