package tech.parasol.akka.workshop.stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source, SourceQueueWithComplete}
import akka.stream.{ActorAttributes, ActorMaterializer, OverflowStrategy, Supervision}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

object StreamBufferSize {

  implicit val system = ActorSystem("StreamBufferSize")
  implicit val materializer = ActorMaterializer()
  implicit val ex = system.getDispatcher

  val batchSize = 10
  val bufferSize = 1024 * 4
  val parallelism = 8

  implicit val logger = LoggerFactory.getLogger(this.getClass)

  private val batchMessageDecider: Supervision.Decider = {
    case e: Exception => {
      logger.error(s"[StreamBufferSize] thrown during push message out processing ===> ", e)
      Supervision.Resume
    }
  }



  private val updateMessageStatusSource: Source[(String, Int), SourceQueueWithComplete[(String, Int)]] =
    Source.queue[(String, Int)](bufferSize = bufferSize,
      overflowStrategy = OverflowStrategy.backpressure)


  private val updateMessageStatusQueue = updateMessageStatusSource
    .groupBy(20, string => {string._1})
    .groupedWithin(6, 1.seconds)
    .map(s => {
      println("s ===> " + s.toString())
      val column = s.head._2
      column
    })
    .async
    .map(x => x)
    .mergeSubstreams
    .toMat(Sink.ignore)(Keep.both)
    .withAttributes(ActorAttributes.supervisionStrategy(batchMessageDecider))
    .run()



  private val queue = updateMessageStatusSource
    .groupBy(20, string => {string._1})
    .groupedWithin(6, 1.seconds)
    .map(s => {
      println("s ===> " + s.toString())
      val column = s.head._2
      column
    })
    //.async
    //.map(x => x)
    .mergeSubstreams
    //.toMat(Sink.ignore)(Keep.left)
    .withAttributes(ActorAttributes.supervisionStrategy(batchMessageDecider))
    //.run()
    .runWith(Sink.fold(0)(_ + _))

  def main(args: Array[String]): Unit = {


    updateMessageStatusQueue._1.offer(("W1", 1))
    //Thread.sleep(2000)
    updateMessageStatusQueue._1.offer(("W2", 2))
    //Thread.sleep(2000)
    updateMessageStatusQueue._1.offer(("W1", 3))


    queue
    //queue.map(x => println("x ===>" + x.toString))


  }
}
