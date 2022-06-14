package tech.parasol.akka.workshop.mailbox

import akka.actor.{Actor, ActorSystem, PoisonPill, Props, Stash, UnrestrictedStash}
import akka.event.{Logging, LoggingAdapter}

import scala.concurrent.ExecutionContextExecutor

class LoggerActor extends Actor {
  val log: LoggingAdapter = Logging(context.system, this)

  def receive: Receive = {
    case "test" => {
      log.info("zzzzz")
      self ! "seq"
    }
    case x => log.info(x.toString)
  }
}

object PriorityMailboxTest {

  implicit val system = ActorSystem("PriorityMailboxTest")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher


  def main(args: Array[String]): Unit = {
    val actor = system.actorOf(Props(classOf[LoggerActor]).withMailbox("prio-dispatcher"))

    val actor1 = system.actorOf(Props(classOf[MessageActor]).withMailbox("prio-dispatcher"))

    actor ! "test"
    actor ! "lowpriorityactor"
    actor ! "lowpriority"
    actor ! "highpriority"
    Thread.sleep(20000)
    actor ! "pigdog"
    actor ! "highpriority"

    actor ! PoisonPill
  }
}
