package tech.parasol.akka.workshop.cluster

import akka.actor.Actor
import org.slf4j.LoggerFactory
import tech.parasol.akka.workshop.cluster.ProfileAction.{AddShare, GetProfile, GetShare, RemoveShare}

import scala.util.{Failure, Success, Try}
import akka.pattern.pipe
import akka.pattern.ask
import akka.actor.Stash
import scala.concurrent.Future

class ProfileActor extends Actor with Stash {

  val logger = LoggerFactory.getLogger(this.getClass.getName)

  implicit val ec = context.system.dispatcher



  private var profileId = ""

  private var sharedList: Map[String, SharedInfo] = Map.empty[String, SharedInfo]

  private var userList: Map[String, User] = Map.empty[String, User]

  Utils.loadSharedInfo.pipeTo(self)

  override def preStart(): Unit = {
    logger.info("ProfileActor start ===> " + self)
    profileId = self.path.name



    /**
     * add some logic to load data from the persistent
     * Event Source
     * ES -> CASSANDRA
     * CQRS
     * CUD COMMAMD STAFUL ACTOR
     * R Q STATELESS ACTOR -> ROUTER POOL ROUTER GROUP ->
     * DDD
     *
     *
     */
  }

  private def map2Seq = {
    sharedList.toSeq.map(_._2)
  }

  def receive: Receive = init


  def init: Receive = {
    case shares: Seq[SharedInfo] => {
      shares.map(share => sharedList += share.shareId -> share)
      logger.info("shares done")
      unstashAll()
      context.become(running)
    }
    case msg => stash()
  }

  def running: Receive = {

    case shares: Seq[SharedInfo] => {
      shares.map(share => sharedList += share.shareId -> share)
      logger.info("shares done")
    }

    case user@User(userId, userName) => {
      logger.info(s"Add user: userId = ${userId}, userName = ${userName}")
      userList += user.userId -> user
      sender ! user
    }

    case AddShare(share) => {
      val caller = sender()
      Try {
        sharedList += share.shareId -> share


        /**
         * SAVE Elastic
         *
         * send kafka pulsar
         *
         */
      } match {
        case Success(_) => caller ! true
        case Failure(e) => {
          caller ! false
        }
      }
    }


    case RemoveShare(shareId) => {
      logger.info("RemoveShare ===> shareId = " + shareId)
      val caller = sender()
      Try {
        sharedList -= shareId
      } match {
        case Success(_) => caller ! true
        case Failure(e) => {
          caller ! false
        }
      }
    }

    case GetProfile(profileId) => {
      //Thread.sleep(3000)
      logger.info("GetProfile ===> " + profileId)
      val profile = ProfileInfo(
        profileId,
        userList.toSeq.map(_._2),
        sharedList.toSeq.map(_._2)
      )
      sender ! profile
    }

    case GetShare(_) => {
      sender ! map2Seq
    }

    case _ => {
      println(s"Unknown message")
    }
  }
}
