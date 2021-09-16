package tech.parasol.akka.workshop.mailbox

import akka.actor.Actor
import org.slf4j.LoggerFactory

final case class MyException(msg: String) extends Exception(msg)

class MessageActor extends Actor {

  val logger = LoggerFactory.getLogger(this.getClass.getName)

  var actorPathName: String = _

  override def preStart(): Unit = {
    logger.info("MessageActor start ===> " + self)
    actorPathName = self.path.name
  }


  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    logger.info("MessageActor preRestart, path = " + self.path + ", object hash = " + this.hashCode)
    super.preRestart(reason, message)
  }


  def receive = {
    case "Sleep" => {
      logger.info("processing message ===> Sleep")
      Thread.sleep(5000)
    }

    case "Exception" => {
      throw MyException("Try throw exception.")
    }
    case msg: String => {
      //if(actorPathName == "$a" || actorPathName == "$b") Thread.sleep(5000)
      logger.info(s"[$actorPathName] is processing message ===> " + msg)

    }
    case _ => {
      println(s"Unknown message")
    }
  }
}