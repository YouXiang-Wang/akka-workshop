package tech.parasol.akka.workshop.persistence


import akka.actor.Props
import akka.pattern.pipe
import akka.persistence.{PersistentActor, SnapshotOffer}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

case class Cmd(data: String)
case class Evt(data: String)

case class ExampleState(events: List[String] = Nil) {
  def updated(evt: Evt): ExampleState = copy(evt.data :: events)
  def size: Int = events.length
  override def toString: String = events.reverse.toString
}


class ResourceWorkloadScheduler extends PersistentActor {
  implicit val system = context.system
  implicit val executionContext: ExecutionContext = system.dispatcher

  val logger = LoggerFactory.getLogger(this.getClass.getName)

  override def persistenceId: String = self.path.name

  override def preStart(): Unit = {
    logger.info(s"ResourceSynchronizer ===> ${self.path.name}")
    super.preStart()
  }

  private def load = {
    Future.successful(true)
  }

  load.pipeTo(self)

  var state = ExampleState()

  def updateState(event: Evt): Unit =
    state = state.updated(event)

  def numEvents = state.size

  val receiveRecover: Receive = {
    case evt: Evt                                 => updateState(evt)
    case SnapshotOffer(_, snapshot: ExampleState) => state = snapshot
  }


  //override def receiveCommand: Receive = initState

  val snapShotInterval = 2

  val receiveCommand: Receive = processingState

  def processingState1: Receive = {
    case Cmd(data) =>
      persist(Evt(s"${data}-${numEvents}")) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
          saveSnapshot(state)
      }
    case "print" => println(state)
  }

  def initState: Receive = {
    case _: Boolean => {
      unstashAll()
      context.become(processingState)
    }
    case _ => stash()
  }


  def processingState: Receive = {

    case c: String =>
      //persistAsync(s"$c-1-outer") { outer1 =>println("message ===> " + c)}
      persistAsync(Evt(s"${c}-${numEvents}")) { event =>
        updateState(event)
        if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0) {
          println("saveSnapshot ===> " + c)
          saveSnapshot(state)
        }

      }

    case x => {
      logger.warn(s"[ResourceSynchronizer] Unknown message ===> " + x)
    }
  }
}


object ResourceWorkloadScheduler {
  def props() = Props(classOf[ResourceWorkloadScheduler])
}