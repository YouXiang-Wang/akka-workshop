package tech.parasol.akka.workshop.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ClosedShape, FlowShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}
import GraphDSL.Implicits._

import scala.concurrent.{ExecutionContextExecutor, Future}

object StreamApp {

  implicit val system = ActorSystem("StreamApp")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val flow1Async: Flow[Int, String, NotUsed] = Flow[Int].mapAsyncUnordered(10)(i => Future {
    s"Number value = ${i} by pass flow1"
  }).async

  val flow2Async: Flow[Int, String, NotUsed] = Flow[Int].map(i => s"Number value = ${i} by pass flow2").async


  def s1 = {

    Source(1 to 5).map(_.toString).runForeach(println)
  }

  def s2 = {
    val source = Source(1 to 5)
    val flow = Flow[Int].map(_.toString)
    val sink = Sink.foreach[String](println)
    val runnable = source.via(flow).to(sink)
    runnable.run()

  }

  def streamBroadcastMerge = {
    val source = Source(1 to 100)

    val graphFlow = Flow.fromGraph(GraphDSL.create() { implicit builder =>
      val flow0: Flow[Int, Int, NotUsed] = Flow[Int].map(i => i)
      val flow1: Flow[Int, Int, NotUsed] = Flow[Int].map(i => i * 2)
      val flow2: Flow[Int, String, NotUsed] = Flow[Int].map(i => s"flow1 = ${i}")
      val flow3: Flow[Int, String, NotUsed] = Flow[Int].map(i => s"flow2 = ${i}")
      val flow4: Flow[String, String, NotUsed] = Flow[String].map(s => s"Stream ${s} is end.")
      val inStage = builder.add(flow0)
      val broadcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[String](2))
      val outStage = builder.add(flow4)

      inStage ~> flow1 ~> broadcast.in
                                       broadcast ~> flow2 ~> merge
                                       broadcast ~> flow3 ~> merge
                                                                   merge ~> outStage
      FlowShape(inStage.in, outStage.out)
    })

    val sink = Sink.foreach(println)
    source.via(graphFlow).runWith(sink)
  }

  def main(args: Array[String]): Unit = {

    streamBroadcastMerge


  }
}
