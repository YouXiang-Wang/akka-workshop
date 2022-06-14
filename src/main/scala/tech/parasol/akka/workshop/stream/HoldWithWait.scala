package tech.parasol.akka.workshop.stream

import akka.stream._
import akka.stream.stage._

import scala.reflect.ClassTag

final class HoldWithWait[T] extends GraphStage[FlowShape[T, T]] {
  val in = Inlet[T]("HoldWithWait.in")
  val out = Outlet[T]("HoldWithWait.out")

  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private var currentValue: T = _
    private var waitingFirstValue = true

    setHandlers(
      in,
      out,
      new InHandler with OutHandler {
        override def onPush(): Unit = {
          currentValue = grab(in)

          println("currentValue ===> " + currentValue)

          if(currentValue.toString == "1") {
            val s = "0".asInstanceOf[T]
            push(out, s)
            currentValue = s
          } else {
            push(out, currentValue)
          }


          /*
          if (waitingFirstValue) {
            waitingFirstValue = false
            if (isAvailable(out)) push(out, currentValue)
          }


          pull(in)

           */
        }

        override def onPull(): Unit = {
          //if(!waitingFirstValue)
            //push(out, currentValue)

          pull(in)
        }
      })

    override def preStart(): Unit = {
      //pull(in)
    }
  }
}