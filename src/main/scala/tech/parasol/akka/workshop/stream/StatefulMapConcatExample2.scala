package tech.parasol.akka.workshop.stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import scala.util.Random._
import scala.math.abs


//encapsulating your input
case class IdentValue(id: Int, value: String)

object StatefulMapConcatExample2 {

  implicit val system = ActorSystem("StatefulMapConcatExample2")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val identValues = List.fill(20)(IdentValue(abs(nextInt()) % 5, "valueHere"))

  val stateFlow = Flow[IdentValue].statefulMapConcat{ () =>
    //state with already processed ids
    var ids = Set.empty[Int]
    identValue => if (ids.contains(identValue.id)) {
      //save value to DB
      println(identValue.value)
      List(identValue)
    } else {
      //save both to database
      println(identValue)
      ids = ids + identValue.id
      List(identValue)
    }
  }

  def main(args: Array[String]): Unit = {

    Source(identValues)
      .via(stateFlow)
      .runWith(Sink.seq)
      .onSuccess { case identValue => println(identValue) }


  }
}
