package tech.parasol.akka.workshop.stream.kafka

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Consumer, Producer, SendProducer}
import akka.kafka._
import akka.stream.scaladsl.Flow
import akka.stream.{ActorAttributes, Supervision}
import akka.util.ByteString
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer, StringSerializer}
import org.slf4j.LoggerFactory
import tech.parasol.akka.workshop.cluster.{Activity, ProfileInfo, SyncTaskMessage}
import tech.parasol.akka.workshop.utils.JsonUtil

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

class MessageProcessor(
                        val consumerTopicConfig: KafkaTopicConsumeConfig,
                        val producerTopicConfig: KafkaTopicProduceConfig,
                        val failedDataConfig: KafkaTopicProduceConfig,
                        val decider: Supervision.Decider = ProcessorUtils.decider)(implicit val system: ActorSystem,
                                    val executionContext: ExecutionContextExecutor) extends ProcessGraph[SyncTaskMessage, SyncTaskMessage, ProfileInfo, Activity]{


  val logger = LoggerFactory.getLogger(this.getClass.getName)

  def maxBatch: Int = 10
  def parallelism: Int = 10
  def retries: Int = 5
  def bufferSize: Int = 1024

  val dataInTopic: String = consumerTopicConfig.topic
  val dataOutTopic: String = producerTopicConfig.topic
  val failedTopic: String = failedDataConfig.topic

  val committerDefaults = CommitterSettings(system)

  protected def transformInput(data: SyncTaskMessage): SyncTaskMessage = {
    data
  }

  protected def fetchRestData(data: SyncTaskMessage): Future[Option[String]] = {
    val uri = s"http://127.0.0.1:18090/profile/${data.profileId}"
    val request = HttpRequest(method = HttpMethods.GET,
      uri = uri
    )

    val responseFut = Http().singleRequest(request)

    responseFut.flatMap(response => {
      if(response.status.isSuccess()) {
        response.entity.withoutSizeLimit.dataBytes.runFold(ByteString.empty) {
          case (acc, b) => acc ++ b
        }.map { s =>
          val content = s.utf8String
          Option(content)
        }
      } else {
        logger.error(s"Error in request fetch API, code = ${response.status.intValue()}")
        Future.successful(None)
      }
    })
  }

  protected def convertResult(m1: SyncTaskMessage, jsonStr: String): Option[ProfileInfo] = {
    Try(JsonUtil.convert[ProfileInfo](jsonStr)) match {
      case Success(v) => {
        Option(v)
      }
      case Failure(e) => {
        logger.error("convertResult exception ===> " + e)
        None
      }
    }
  }

  protected def transformOutput(data: ProfileInfo): Seq[Activity] = {
    val activity = Activity(
      data.profileId,
      Option(data)
    )
    Seq(activity, activity)
  }

  val consumerSettings = {
    val kafkaServerConfig: KafkaServerConfig = consumerTopicConfig.kafkaServerConfig
    val bootstrapServers: String = kafkaServerConfig.bootstrapServers
    val config = system.settings.config.getConfig("akka.kafka.consumer")
    val groupId: String = consumerTopicConfig.groupId
    val offsetReset: String = consumerTopicConfig.offsetReset
    ConsumerSettings(config, new StringDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withGroupId(groupId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset)
  }


  val producerSettings = {
    val kafkaServerConfig: KafkaServerConfig = producerTopicConfig.kafkaServerConfig
    val config = system.settings.config.getConfig("akka.kafka.producer")
    ProducerSettings(config, new StringSerializer, new StringSerializer)
      .withBootstrapServers(kafkaServerConfig.bootstrapServers)
  }


  val producer = SendProducer(producerSettings)(system)

  protected def extractMessageKey(message: String): Option[String] = None


  def handle(message: AnyRef): Boolean = {
    true
  }

  def process(message: ConsumerMessage.CommittableMessage[String, Array[Byte]]): Future[CommittableProcessDataMessage] = {
    val value = message.record.value()
    val content = new String(value, "UTF-8")
    if(content != null && content.length > 0) {
      val committableProcessDataMessage = Try(JsonUtil.convert[SyncTaskMessage](content)) match {
        case Success(v) => {
          CommittableProcessDataMessage(
            true,
            Option(content),
            None,
            v,
            None,
            message.committableOffset
          )
        }
        case Failure(e) => {
          logger.error("[KafkaTopicConsumeSourceGraph] error", e)
          CommittableProcessDataMessage(
            false,
            Option(content),
            None,
            message,
            None,
            message.committableOffset
          )
        }
      }
      Future.successful(committableProcessDataMessage)
    } else {

      /**
       *
       * commit the record directly
       */

      val data = CommittableProcessDataMessage(
        true,
        None,
        None,
        None,
        None,
        message.committableOffset
      )
      Future.successful(data)
    }
  }

  protected def flow: Flow[CommittableProcessDataMessage, CommittableProcessDataMessage, NotUsed] = processFlow

  protected def createRecord(dataOutTopic: String, value: String): ProducerRecord[String, String] = {
    val keyOpt = extractMessageKey(value)
    if(keyOpt.isDefined) new ProducerRecord[String, String](dataOutTopic, keyOpt.get, value) else new ProducerRecord[String, String](dataOutTopic, value)
  }

  Consumer
    .committableSource(consumerSettings, Subscriptions.topics(dataInTopic))
    .mapAsyncUnordered(parallelism) { msg =>
      process(msg)
    }
    .async
    .via(flow)
    .async
    .mapAsyncUnordered(parallelism) { result =>
      val envelope = if(result.successful) {
        result.dataMessage match {
          case seq: Seq[AnyRef] => {
            val records = seq.map(r => {
              val v = if(r.isInstanceOf[String]) r.toString else JsonUtil.toJson(r)
              createRecord(dataOutTopic, v)
            }).to[collection.immutable.Seq]

            ProducerMessage.multi(
              records,
              result.offset
            )
          }

          case v: String => {
            ProducerMessage.single(createRecord(dataOutTopic, v), result.offset)
          }

          case _ => {
            ProducerMessage.single(createRecord(dataOutTopic, JsonUtil.toJson(result.dataMessage)), result.offset)
          }
        }
      } else {
        val content = result.originalMessage.getOrElse("")
        logger.info(s"Data processed failed ==> will send to the topic=[${failedTopic}] to reconsume again. content = ${content}")
        ProducerMessage.single(new ProducerRecord[String, String](failedTopic, content), result.offset)
      }
      Future(envelope)(executionContext)
    }.async

    /**
     * withMaxBatch means how many offsets would be commit once
     * 1: each message commit once
     */

    .toMat(Producer.committableSink(producerSettings, committerDefaults.withMaxBatch(maxBatch)))(DrainingControl.apply)
    .withAttributes(ActorAttributes.supervisionStrategy(decider))
    .run()


}
