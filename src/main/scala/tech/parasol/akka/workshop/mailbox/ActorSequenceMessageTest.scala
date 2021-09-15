package tech.parasol.akka.workshop.mailbox

import akka.actor.ActorSystem
import tech.parasol.akka.workshop.utils.CommonUtils


object ActorSequenceMessageTest {

  val system = ActorSystem("ActorSequenceMessageTest")



  def testSingle = {
    val actorRef = CommonUtils.createActor(system, classOf[MessageActor])
    actorRef ! "1"
    actorRef ! "2"
    actorRef ! "3"
  }


  def testDouble = {
    val actorRef1 = CommonUtils.createActor(system, classOf[MessageActor])
    val actorRef2 = CommonUtils.createActor(system, classOf[MessageActor])
    actorRef1 ! "1"
    actorRef1 ! "2"
    actorRef1 ! "3"

    actorRef2 ! "1"
    actorRef2 ! "2"
    actorRef2 ! "3"
  }


  def main(args: Array[String]): Unit = {
    testDouble
  }

}
