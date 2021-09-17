package tech.parasol.akka.workshop.stream.kafka

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.kafka.ConsumerMessage
import akka.stream.{FlowShape, Graph, OverflowStrategy}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition}
import akka.util.ByteString
import org.slf4j.Logger

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

trait ProcessGraph[Input, M1 <: AnyRef, M2 <: AnyRef, Output] {

  implicit val system: ActorSystem

  type OriginalMessage = Option[String]

  def retries: Int
  def bufferSize: Int
  def parallelism: Int

  val logger: Logger

  implicit val executionContext: ExecutionContextExecutor

  protected def transformInput(data: Input): M1

  protected def convertResult(m1: M1, jsonStr: String): Option[M2]

  protected def transformOutput(data: M2): Seq[Output]

  private val fetchStatusCnt = 2
  private val resMergeStageCnt = 2


  protected def fetchRestData(data: M1): Future[Option[String]]

  private def fetchDataStatusPartition = Partition[(M1, Option[String], ConsumerMessage.CommittableOffset, OriginalMessage)](fetchStatusCnt, partitionFunc)

  private def resultMerge = Merge[(Seq[Output], ConsumerMessage.CommittableOffset, OriginalMessage)](resMergeStageCnt)

  private def inputTransformFlow: Flow[CommittableProcessDataMessage, (M1, ConsumerMessage.CommittableOffset, OriginalMessage), _] = Flow[CommittableProcessDataMessage]
    .buffer(bufferSize, OverflowStrategy.backpressure)
    .mapAsyncUnordered(parallelism)(message => Future {
      val input = message.dataMessage.asInstanceOf[Input]
      val m1 = transformInput(input)
      (m1, message.offset, message.originalMessage)
    }).async

  private def fetchRestDataFlow: Flow[(M1, ConsumerMessage.CommittableOffset, OriginalMessage), (M1, Option[String], ConsumerMessage.CommittableOffset, OriginalMessage), _] = Flow[(M1, ConsumerMessage.CommittableOffset, OriginalMessage)]
    .mapAsyncUnordered(parallelism)(message => {
      fetchRestData(message._1).map(result => {
        (message._1, result, message._2, message._3)
      })
    })
    .async

  protected def partitionFunc: ((M1, Option[String], ConsumerMessage.CommittableOffset, OriginalMessage)) => Int =
    tup => {
      tup._2 match {
        case None => {
          0
        }
        case Some(_) => {
          1
        }
      }
    }

  private def processFailedFlow: Flow[(M1, Option[String], ConsumerMessage.CommittableOffset, OriginalMessage), (Seq[Output], ConsumerMessage.CommittableOffset, OriginalMessage), _] = Flow[(M1, Option[String], ConsumerMessage.CommittableOffset, OriginalMessage)].map( res =>
    (Seq.empty, res._3, res._4)
  )

  private def dataTransferFlow: Flow[(M1, Option[String], ConsumerMessage.CommittableOffset, OriginalMessage), (Option[M2], ConsumerMessage.CommittableOffset, OriginalMessage), _] = Flow[(M1, Option[String], ConsumerMessage.CommittableOffset, OriginalMessage)]
    .mapAsyncUnordered(parallelism)(message => Future {
      val m2opt = Try(convertResult(message._1, message._2.get)) match {
        case Success(value) => {
          value
        }
        case Failure(e) => {
          logger.error("[dataTransferFlow] exception ===>" , e)
          None
        }
      }
      (m2opt, message._3, message._4)
    }).async

  private def outputTransformFlow: Flow[(Option[M2], ConsumerMessage.CommittableOffset, OriginalMessage), (Seq[Output], ConsumerMessage.CommittableOffset, OriginalMessage), _] = Flow[(Option[M2], ConsumerMessage.CommittableOffset, OriginalMessage)]
    .mapAsyncUnordered(parallelism)(r => Future {
      val dataOpt = r._1
      val res = if(dataOpt.isDefined) {
        Try(transformOutput(dataOpt.get)) match {
          case Success(value) => {
            value
          }
          case Failure(e) => {
            logger.error("[outputTransformFlow] exception ===>" , e)
            Seq.empty
          }
        }
      } else
        Seq.empty
      (res, r._2, r._3)
    }).async

  private def processResultOutFlow: Flow[(Seq[Output], ConsumerMessage.CommittableOffset, OriginalMessage), CommittableProcessDataMessage, _] = Flow[(Seq[Output], ConsumerMessage.CommittableOffset, OriginalMessage)]
    .map(r => {
      val dataOpt = r._1
      val res = if(dataOpt.nonEmpty) {
        CommittableProcessDataMessage(
          true,
          r._3,
          None,
          dataOpt.asInstanceOf[AnyRef],
          None,
          r._2
        )
      } else
        CommittableProcessDataMessage(
          false,
          r._3,
          None,
          None,
          None,
          r._2
        )
      res
    })


  private def processGraph: Graph[FlowShape[CommittableProcessDataMessage, CommittableProcessDataMessage], NotUsed] =
    GraphDSL
      .create() { implicit builder =>
        import GraphDSL.Implicits._

        val inputTransStage = builder.add(inputTransformFlow)
        val fetchRestDataStage = builder.add(fetchRestDataFlow)
        //val balancedFetchRestDataFlow = StreamHelper.balancer[(M1, ConsumerMessage.CommittableOffset, OriginalMessage), (M1, Option[String], ConsumerMessage.CommittableOffset, OriginalMessage)](fetchRestDataFlow, parallelism)
        //val fetchRestDataStage = builder.add(balancedFetchRestDataFlow)
        val fetchDataStatusPartStage = builder.add(fetchDataStatusPartition)
        val processFailedStage = builder.add(processFailedFlow)
        val dataTransferStage = builder.add(dataTransferFlow)
        val outputTransStage = builder.add(outputTransformFlow)
        val resultMergeStage = builder.add(resultMerge)
        val processResultOutStage = builder.add(processResultOutFlow)

        // @formatter:off

        inputTransStage ~> fetchRestDataStage ~> fetchDataStatusPartStage.in
        fetchDataStatusPartStage.in

        fetchDataStatusPartStage.out(0) ~> processFailedStage ~> resultMergeStage
        fetchDataStatusPartStage.out(1) ~> dataTransferStage ~> outputTransStage ~> resultMergeStage

        resultMergeStage ~> processResultOutStage

        // @formatter:on

        FlowShape(inputTransStage.in, processResultOutStage.out)
      }



  def processFlow: Flow[CommittableProcessDataMessage, CommittableProcessDataMessage, NotUsed] =
    Flow.fromGraph(processGraph).named("ProcessGraph")

}
