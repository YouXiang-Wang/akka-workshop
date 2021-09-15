package tech.parasol.akka.workshop
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source}
import GraphDSL.Implicits._
import akka.stream.testkit.scaladsl.{TestSink, TestSource}

import scala.concurrent.{ExecutionContextExecutor, Future}

/*
class GeneralTests extends FunSuite {

  implicit val system = ActorSystem("Test-System")
  implicit val materializer = ActorMaterializer()

  case class Wid(id: Int, v: String)

  test("Substream with folds") {
    val (pub, sub) = TestSource.probe[Wid]
      .groupBy(Int.MaxValue, _.id)
      .fold("")((a: String, b: Wid) => a + b.v)
      .grouped(2)
      .mergeSubstreams
      .toMat(TestSink.probe[Seq[String]])(Keep.both)
      .run()

    sub.request(5)
    pub.sendNext(Wid(1,"1"))
    pub.sendNext(Wid(2,"2"))
    sub.expectNext()
    pub.sendNext(Wid(3,"3"))
    pub.sendNext(Wid(4,"4"))
    pub.sendNext(Wid(5,"5"))
    sub.expectNext()
    sub.expectNext()
  }
}

 */