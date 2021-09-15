package tech.parasol.akka.workshop.mailbox

import akka.actor.{ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.routing._
import tech.parasol.akka.workshop.utils.CommonUtils

object MessageRouterTest {

  val system = ActorSystem("MessageRouterTest")

  val strategy = OneForOneStrategy() {
    case _: Exception => {
      println("----------------")
      SupervisorStrategy.Resume
    }
    case _            => SupervisorStrategy.Escalate
  }


  //Props.create(classOf[MessageActor]).withRouter(new SmallestMailboxPool(50000))

  def poolRouter(mode: Int = 1) = {

    val router = mode match {
      case 0 => {
        system.actorOf(
          Props.create(classOf[MessageActor]).withRouter(new BalancingPool(3).withSupervisorStrategy(strategy)))
      }
      case 1 => {
        system.actorOf(
          Props.create(classOf[MessageActor]).withRouter(new RoundRobinPool(3).withSupervisorStrategy(strategy)))
      }
      case 2 => {
        system.actorOf(
          Props.create(classOf[MessageActor]).withRouter(new RandomPool(3)))
      }
      case 3 => {
        system.actorOf(
          Props.create(classOf[MessageActor]).withRouter(new SmallestMailboxPool(3)))
      }

      case 4 => {
        system.actorOf(
          Props.create(classOf[MessageActor]).withRouter(new SmallestMailboxPool(3)))
      }
      case 5 => {
        system.actorOf(
          Props.create(classOf[MessageActor]).withRouter(new BroadcastPool(3)))
      }

      case 10 => {
        val actors = (1 to 3).map(i => {
          system.actorOf(Props.create(classOf[MessageActor]), s"actor_${i.toString}")
        })
        val paths = actors.toList.map(actor => actor.path.toString)
        system.actorOf(RoundRobinGroup(paths).props())

      }
      case 11 => {
        val actors = (1 to 3).map(i => CommonUtils.createActor(system, classOf[MessageActor], s"actor_${i.toString}"))
        val paths = actors.toList.map(actor => actor.path.toString)
        system.actorOf(RandomGroup(paths).props())
      }

      case 12 => {
        val actors = (1 to 3).map(i => CommonUtils.createActor(system, classOf[MessageActor], s"actor_${i.toString}"))
        val paths = actors.toList.map(actor => actor.path.toString)
        system.actorOf(BroadcastGroup(paths).props())
      }
      case _ => {
        system.actorOf(
          Props.create(classOf[MessageActor]).withRouter(new RoundRobinPool(3)))
      }

    }

    /**
     * val router1 = system.actorOf(FromConfig.props(Props(classOf[MessageActor])),"balance-pool-router")
     */


    router
  }

  def testRouter = {

    val router = system.actorOf(
      FromConfig.props(Props(classOf[MessageActor]))
      ,"balance-pool-router")

    val router2 = system.actorOf(
      Props.create(classOf[MessageActor]).withRouter(new RoundRobinPool(3))) //.withSupervisorStrategy(strategy)))

    (1 to 10).map(
      i => {
        router2 ! akka.routing.Broadcast(i.toString)
        //router2 ! i.toString
      }
    )

  }


  def broadcast(actorRef: ActorRef) = {
    (1 to 10).map(
      i => {
        actorRef ! akka.routing.Broadcast(i.toString)
      }
    )
  }

  def tellMessage(actorRef: ActorRef, count: Int = 10) = {
    (1 to count).map(
      i => {
        actorRef ! i.toString
      }
    )
  }



  def main(args: Array[String]): Unit = {
    val actorRef = poolRouter(1)

    actorRef ! "Exception"
    // tell
    //tellMessage(actorRef, 100)

    //broadcast
    //broadcast(actorRef)



  }

}
