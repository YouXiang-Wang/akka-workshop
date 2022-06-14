package tech.parasol.akka.workshop.stream


import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.{ActorAttributes, ActorMaterializer, OverflowStrategy, Supervision}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, SourceQueueWithComplete}
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

case class Foo(id: String, value: String)

object GroupedWithIn {
  implicit val system = ActorSystem("akka-streams-oom")
  implicit val materializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {
    println("starting tests...")
    val attempt = Try(forceOOM)

    attempt match {
      case Success(_) => println("all tests passed successfully")
      case Failure(e) => println(s"exception: e.getMessage")
    }

    println("terminating system...")
    system.terminate
    println("system terminated")
    println("done with tests...")
  }

  private def forceOOM: Unit = {
    println("executing forceOOM...")
    val sink = Sink.fold[Int, Int](0)(_ + _)

    val future =
      bigSource
        .map(logEmit)
        .via(slowSubscriber)
        .runWith(sink)

    val finalResult = Await.result(future, Duration.Inf)
    println(s"forceOOM result: $finalResult")
  }

  private def bigSource = {
    val largeIterator = () =>
      Iterator
        .from(0,1000000000)
        .map(_ => generateLargeFoo)

    Source.fromIterator(largeIterator)
  }

  private def slowSubscriber =
    Flow[Foo]
      .map { foo =>
        println(s"allocating memory for ${foo.id} at ${time}")
        Foo(foo.id, bloat)
      }
      .async  // if i remove this, the 5 second window below doesn't seem to work
      .groupedWithin(100, 5.seconds)
      .map(foldFoos)

  private def logEmit(x: Foo): Foo = {
    println(s"emitting next record: ${x.id} at ${time}")
    x
  }

  private def foldFoos(x: Seq[Foo]): Int = {
    println(s"folding records                                            at ${time}")
    x.map(_.value.length).fold(0)(_ + _)
  }

  private def time: String = LocalDateTime.now.toLocalTime.toString

  private def bloat: String = {
    (0 to 10)
      .map(_ => generateLargeFoo.value)
      .fold("")(_ + _)
  }

  private def generateLargeFoo: Foo = {
    Foo(java.util.UUID.randomUUID.toString, (0 to 1000000).mkString)
  }
}