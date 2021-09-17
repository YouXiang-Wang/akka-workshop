package tech.parasol.akka.workshop.stream.kafka

import akka.kafka.ConsumerMessage


final case class KafkaServerConfig(
                                    bootstrapServers: String,
                                    jaasConfig: Option[String] = None,
                                    scyProtocol: Option[String] = None,
                                    saslMechanism: Option[String] = None

                                  )

/*
final case class KafkaTopicConsumeConfig(
                                          topic: String,
                                          groupId: String,
                                          offsetReset: String = "earliest",
                                          topicKey: Option[String] = None,
                                          handler: Option[String] = None,
                                          method: Option[String] = None,
                                          async: Boolean = true,
                                          parallelism: Int = Runtime.getRuntime.availableProcessors + 1
                                        )

 */

final case class KafkaTopicConsumeConfig(
                                          kafkaServerConfig: KafkaServerConfig,
                                          topic: String,
                                          groupId: String,
                                          offsetReset: String = "latest",
                                          parallelism: Int = Runtime.getRuntime.availableProcessors + 1
                                        )


final case class KafkaTopicProduceConfig(
                                          kafkaServerConfig: KafkaServerConfig,
                                          topic: String
                                        )



final case class TopicMessage(topic: String,
                              key: Option[String],
                              content: String,
                              offset: Option[ConsumerMessage.CommittableOffset] = None) {


                              }
final case class CommittableProcessDataMessage(successful: Boolean,
                                               originalMessage: Option[String] = None,
                                               key: Option[String] = None,
                                               dataMessage: AnyRef,
                                               errorMsg: Option[String] = None,
                                               offset: ConsumerMessage.CommittableOffset) {

  /**
   * for java
   */

  def getSuccessful = successful
  def getId = originalMessage
  def getErrorMsg = errorMsg
  def getOffset = offset


}



