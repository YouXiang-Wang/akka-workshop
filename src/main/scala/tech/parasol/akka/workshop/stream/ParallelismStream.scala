package tech.parasol.akka.workshop.stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.{ExecutionContextExecutor, Future}


object ParallelismStream {


  implicit val system = ActorSystem("ParallelismStream")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def spin(value: Int): Int = {
    val start = System.currentTimeMillis()
    while ((System.currentTimeMillis() - start) < 10) {}
    value
  }


  val parallelism = 1

  def test1 = {
    val start = System.currentTimeMillis()
    Source(1 to 1000)
      .mapAsync(parallelism) {x => Future(spin(x))}
      //.map(spin)
      //.map(spin)
      .mapAsync(parallelism) {x => Future(spin(x))}
      .runWith(Sink.ignore)
      .onComplete(_ => {
        println("Consuming ===> " + (System.currentTimeMillis() - start))
        system.terminate()
      })
  }

  def test2 = {
    val start = System.currentTimeMillis()
    Source(1 to 1000)
      //.map(spin)
      .mapAsync(parallelism) {x => Future(spin(x))}
      .async
      //.map(spin)
      .mapAsync(parallelism) {x => Future(spin(x))}
      .runWith(Sink.ignore)
      .onComplete(_ => {
        println("Consuming ===> " + (System.currentTimeMillis() - start))
        system.terminate()
      })
  }

  def main(args: Array[String]): Unit = {
    //test1
    test1
  }
}
