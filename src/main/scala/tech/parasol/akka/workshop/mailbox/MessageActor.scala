package tech.parasol.akka.workshop.mailbox

import akka.actor.Actor
import org.slf4j.LoggerFactory



class SequenceActor extends Actor {

  val logger = LoggerFactory.getLogger(this.getClass.getName)

  override def preStart(): Unit = {
    logger.info("UserActor start ===> " + self)
  }


  def receive = {

    case msg: String => {
      logger.info("processing message ===> " + msg)
      Thread.sleep(5000)
    }

    case _ => {
      println(s"Unknown message")
    }
  }
}