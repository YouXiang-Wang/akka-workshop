package tech.parasol.akka.workshop.stream

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.util.{Failure, Success}

object StreamConditionComplete {

  implicit val system = ActorSystem("StreamConditionComplete")
  implicit val materializer = ActorMaterializer()
  implicit val ex = system.getDispatcher


  def flow1 = {
    val (queue, result) =
      Source.queue[Int](bufferSize = 1, OverflowStrategy.fail)
        .toMat(Sink.foreach(println))(Keep.both)
        .run()

    val upstream = Source.fromIterator(() => (1 to 10).iterator)
      .map(i => if(i == 5) throw new RuntimeException("Boom!") else i)
      .toMat(Sink.foreachAsync(parallelism = 1) { i =>
        queue.offer(i)
          .map( _ => ())
      })(Keep.right)
      .run()


    upstream.onComplete {
      case Failure(exception) =>
        queue.offer(-1)
          .map {
            _ => queue.fail(exception)
          }

      case Success(value) =>
        queue.offer(0)
          .map {
            _ => queue.complete()
          }
    }
  }




  def main(args: Array[String]): Unit = {
    flow1
  }

}

