package tech.parasol.akka.workshop.stream


import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, Zip}
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContextExecutor, Future}

object SimpleZipStream {

  implicit val system = ActorSystem("SimpleZipStream")
  implicit val materializer = ActorMaterializer()

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val logger = LoggerFactory.getLogger(getClass.getName)

  def stream1 = {
    Source (1 to 5).map( _.toString).runForeach(println(_))
  }

  def stream2 = {
    val source = Source (1 to 5)
    val flow = Flow[Int].map (_.toString)
    val sink = Sink.foreach[String](println)
    val runnable = source.via(flow).to(sink)
    runnable.run()
  }


  val svcFlow1Async: Flow[Int, String, NotUsed] = Flow[Int].mapAsyncUnordered(100)(i => Future {
    if(i % 2 == 0) {
      logger.info(s"i = ${i}")
      Thread.sleep(5000)
    }
    (i).toString
  }).async

  val svcFlow2Async: Flow[Int, Long, NotUsed] = Flow[Int].map(i => (i).toLong).async


  def stream3 = {

    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2))
      val zip = builder.add(Zip[String, Long])

      Source((1 to 100).toList) ~> broadcast.in

      broadcast ~> svcFlow1Async ~> zip.in0
      broadcast ~> svcFlow2Async ~> zip.in1
      zip.out ~> Sink.foreach(println)
      ClosedShape
    }).run()
  }


  def stream4 = {
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      Source((1 to 100).toList).async ~> svcFlow1Async ~> Sink.foreach(println)
      ClosedShape
    }).run()

  }

  def svcFlow = {
    Flow
      .fromGraph(
        GraphDSL.create() { implicit builder =>

          import GraphDSL.Implicits._

          val flow = Flow[Int].map(x => x)
          val inputTransStage = builder.add(flow)

          val broadcast = builder.add(Broadcast[Int](2))

          val zip = builder.add(Zip[String, Long])

          broadcast ~> svcFlow1Async ~> zip.in0
          broadcast ~> svcFlow2Async ~> zip.in1
          FlowShape(inputTransStage.in, zip.out)

        }
      )
  }

  def main(args: Array[String]): Unit = {
    stream3

  }
}
