package tech.parasol.akka.workshop.mailbox



import akka.actor.SupervisorStrategy.{Decider, Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorKilledException, DeathPactException, OneForOneStrategy}
import org.slf4j.LoggerFactory
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._


class MessageChildActor extends Actor {

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

    case "Exception" => {
      throw MyException("Try throw exception.")
    }


    case _ => {
      println(s"Unknown message")
    }
  }
}