package tech.parasol.akka.workshop.cluster

import akka.actor.{Actor, ActorLogging, Address, InvalidActorNameException, OneForOneStrategy}
import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.cluster.ClusterEvent.{CurrentClusterState, InitialStateAsEvents, MemberEvent, MemberExited, MemberJoined, MemberLeft, MemberRemoved, MemberUp, MemberWeaklyUp, UnreachableMember}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.metrics.ClusterMetricsExtension
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._

class ClusterMetricsActor extends Actor with ActorLogging {

  type ConcurrentMap[A, B] = scala.collection.concurrent.TrieMap[A, B]

  val cluster: Cluster = Cluster(context.system)
  val selfAddress: Address = cluster.selfAddress

  implicit val metricsOn: Boolean = false

  private val members = new ConcurrentMap[String, Member]

  val logger: Logger = LoggerFactory.getLogger(this.getClass.getName)

  override def preStart(): Unit = {
    if(metricsOn) ClusterMetricsExtension(context.system).subscribe(self)
    cluster.subscribe(
      self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember], classOf[MemberUp]
    )
  }

  override def postStop(): Unit = {
    if(metricsOn) ClusterMetricsExtension(context.system).unsubscribe(self)
    cluster.unsubscribe(self)
  }

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case e: InvalidActorNameException =>
        logger.error("Stop after InvalidActorNameException: " + e)
        Stop
      case e: Exception =>
        logger.error("initiating Restart after Exception: " + e)
        Restart
    }

  def receive = memberEventReceive orElse nodeReceive orElse otherReceive

  def memberEventReceive: Receive = {
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up).foreach(logMemberStatus)

    case MemberJoined(member) =>
      handleMemberJoined(member)
      logMemberStatus(member)

    case MemberUp(member) =>
      handleMemberUp(member)
      logMemberStatus(member)

    case MemberWeaklyUp(member) =>
      handleMemberWeaklyUp(member)
      logMemberStatus(member)

    case MemberLeft(member) =>
      handleMemberLeft(member)
      logMemberStatus(member)

    case UnreachableMember(member) =>
      logMemberStatus(member)

    case MemberExited(member) =>
      handleMemberExited(member)
      logMemberStatus(member)

    case MemberRemoved(member, _) =>
      handleMemberRemoved(member)
      logMemberStatus(member)

  }



  protected def nodeReceive: Receive = {
    case msg =>
      logger.info(s"Received message: $msg")
  }

  protected def logMemberStatus(member: Member) = {
    val roles = member.roles.mkString(", ")
    val address = member.address.toString.replace("akka://", "")
    val status = member.status
    val info = s"Node [$address] is [$status] ===> roles are [$roles] "
    logger.info(info)
  }

  protected def handleMemberUp(member: Member): Unit = {
    members.put(member.address.hostPort, member)
  }

  protected def handleMemberRemoved(member: Member): Unit = {
    members.remove(member.address.hostPort)
  }

  protected def handleMemberJoined(member: Member): Unit = {}

  protected def handleMemberExited(member: Member): Unit = {}

  protected def handleMemberLeft(member: Member): Unit = {}

  protected def handleMemberWeaklyUp(member: Member): Unit = {}

  def otherReceive: Receive = {
    case msg =>
      logger.info(s"Received message: $msg")
  }


}

