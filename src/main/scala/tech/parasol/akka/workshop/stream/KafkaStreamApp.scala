package tech.parasol.akka.workshop.stream

import akka.actor.ActorSystem
import tech.parasol.akka.workshop.stream.kafka.{KafkaServerConfig, KafkaTopicConsumeConfig, KafkaTopicProduceConfig, MessageProcessor}

import scala.concurrent.ExecutionContextExecutor

object KafkaStreamApp {

  implicit val system = ActorSystem("KafkaStreamApp")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher


  def start = {

    val kafkaNodes = "127.0.0.1:9092"
    val topic = "process_message_in"
    val outTopic = "process_result_out"
    val failedTopic = "process_failed_message_in"
    val groupId = "message_group"

    val kafkaServerConfig = KafkaServerConfig(kafkaNodes)

    val dataInConfig = KafkaTopicConsumeConfig(
      kafkaServerConfig = kafkaServerConfig,
      topic = topic,
      groupId = groupId
    )

    val dataOutConfig = KafkaTopicProduceConfig(
      kafkaServerConfig = kafkaServerConfig,
      topic = outTopic
    )

    val failedDataConfig = KafkaTopicProduceConfig(
      kafkaServerConfig = kafkaServerConfig,
      topic = failedTopic
    )

    new MessageProcessor(dataInConfig, dataOutConfig, failedDataConfig)

  }

  def main(args: Array[String]): Unit = {
    start

  }
}
