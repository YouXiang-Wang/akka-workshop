package tech.parasol.akka.workshop.stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}

object StatefulMapConcatExample1 {

  implicit val system = ActorSystem("StatefulMapConcatExample1")
  implicit val materializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {

    val stream = Flow[Int].statefulMapConcat {
      () => {

        var state: List[Int] = Nil

        element => {
          state = element :: state
          List(state)
        }
      }
    }

    val groupByFlow =
      Flow[Int]
        .groupBy(1000000, identity, allowClosedSubstreamRecreation = true)
        .via(stream)
        .mergeSubstreams

    Source(List(1,1,2,3,3,3))
      .via(groupByFlow)
      .runForeach(i => println(i))


  }
}
