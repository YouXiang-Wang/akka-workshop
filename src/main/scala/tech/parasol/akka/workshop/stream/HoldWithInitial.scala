package tech.parasol.akka.workshop.stream


import akka.stream._
import akka.stream.stage._
final class HoldWithInitial[T](initial: T) extends GraphStage[FlowShape[T, T]] {
  val in = Inlet[T]("HoldWithInitial.in")
  val out = Outlet[T]("HoldWithInitial.out")

  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private var currentValue: T = initial

    setHandlers(in, out, new InHandler with OutHandler {
      override def onPush(): Unit = {
        currentValue = grab(in)
        pull(in)
      }

      override def onPull(): Unit = {
        push(out, currentValue)
      }
    })

    override def preStart(): Unit = {
      pull(in)
    }
  }

}