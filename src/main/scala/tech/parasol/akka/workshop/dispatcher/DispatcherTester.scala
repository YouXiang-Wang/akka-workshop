package tech.parasol.akka.workshop.dispatcher

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait Message
case class InsertCommand(recordCount:Int) extends Message
case class ControlCommand(message: String, startTime: Long) extends Message
case class StartCommand(actorCount: Int) extends Message
case class ExecutionResult(count: Long) extends Message

class WriterActor extends Actor {
  override def preStart(): Unit = {
    println(Thread.currentThread().getName)
  }

  def receive = {
    case InsertCommand(v) => {
      println("inert!")
      sender() ! 100L
    }
  }
}

class ControlActor(dispatcher: String) extends Actor {
  implicit val timeout = Timeout(5 minutes)

  implicit val executionContext = context.system.dispatchers.lookup(dispatcher)
  def receive = {
    case msg: StartCommand =>
      val startTime = System.currentTimeMillis()
      val actors = createActors(msg.actorCount, dispatcher)
      val results = actors.map(actor => {
        (actor ? InsertCommand(100)).mapTo[Long]
      })
      val aggregate = Future.sequence(results).map(results => ExecutionResult(results.sum))

      aggregate onComplete {
        case Success(result) =>
          val endTime = System.currentTimeMillis()
          val costTime = endTime - startTime
          println(s"It take total time ${costTime} milli seconds, result = ${result.count}")
        case Failure(e) => {
          e.printStackTrace()
          println("It failed.")
        }
      }
  }

  def createActors(count: Int, dispatcher: String): List[ActorRef] = {
    val props = Props(classOf[WriterActor]).withDispatcher(dispatcher)
    (1 to count).map(i => {
      context.actorOf(props, s"writer_$i")
    }).toList
  }
}

object DispatcherTester {
  def main(args: Array[String]): Unit = {
    val system: ActorSystem = ActorSystem("DispatcherTester")

    println("Core size = " + Runtime.getRuntime.availableProcessors())

    /**
     *
     * val dispatcher = "executor.writer-dispatcher"
     * val dispatcher = "executor.pinned-writer-dispatcher"
     * val dispatcher = "forkjoin-writer-dispatcher"
     */

    //val dispatcher = "executor.writer-dispatcher"
    //val dispatcher = "executor.pinned-writer-dispatcher"
    val dispatcher = "executor.forkjoin-writer-dispatcher"
    val actor = system.actorOf(Props(classOf[ControlActor], dispatcher), "controller")

    actor ! StartCommand(100)
  }
}