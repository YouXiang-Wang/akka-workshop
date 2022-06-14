package tech.parasol.akka.workshop.stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

object StreamConcat {

  implicit val system = ActorSystem("StreamConcat")
  implicit val materializer = ActorMaterializer()
  implicit val ex = system.getDispatcher

  def main(args: Array[String]): Unit = {

    val s1 = Source(1 to 10).map(x => {
      Thread.sleep(200)
      x
    })

    val s2 = s1.concat(Source.single(20))

    s2.map(println(_)).runWith(Sink.ignore)

  }
}

