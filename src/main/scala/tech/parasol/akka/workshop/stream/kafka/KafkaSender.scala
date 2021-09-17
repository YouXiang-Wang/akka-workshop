package tech.parasol.akka.workshop.stream.kafka

import java.io.FileNotFoundException
import java.nio.file.NoSuchFileException

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{ActorAttributes, OverflowStrategy, Supervision}
import com.fasterxml.jackson.core.JsonParseException
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._

object KafkaSender {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.parseString("akka.log-config-on-start = on")
      .withFallback(ConfigFactory.parseString(s"akka.remote.artery.canonical.port = ${25521}"))
      .withFallback(ConfigFactory.parseString(s"akka.http.server.request-timeout = 60 s"))
      .withFallback(ConfigFactory.load())

    implicit val system = ActorSystem("KafkaSender", config)


    import system.dispatcher

    implicit val logger = LoggerFactory.getLogger(this.getClass.getName)


    val bootstrapServers: String = "127.0.0.1:9092"


    val topic = "process_message_in"
    val producerSettings =
      ProducerSettings(system.settings.config.getConfig("akka.kafka.producer"),
        new StringSerializer, new StringSerializer)
        .withBootstrapServers(bootstrapServers)

    val kafkaProducer = producerSettings.createKafkaProducer()

    val producerQueue = Source
      .queue[String](128, OverflowStrategy.backpressure)
      .map(str => new ProducerRecord[String, String](topic, str))
      .toMat(Producer.plainSink(producerSettings, kafkaProducer))(Keep.left)
      .withAttributes(ActorAttributes.supervisionStrategy(ProcessorUtils.decider))
      .run()


    val message = """{"taskId":"111111","profileId":"profile_1"}"""

    Source(1 to 1000)
      .throttle(1, 2.seconds)
      .runForeach(_ => producerQueue.offer(message))
      .onComplete(_ => system.terminate())

  }

}