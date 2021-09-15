package tech.parasol.akka.workshop.cluster

import akka.actor.Actor
import org.slf4j.LoggerFactory
import tech.parasol.akka.workshop.cluster.ProfileAction.{AddShare, GetProfile, GetShare, RemoveShare}

import scala.util.{Failure, Success, Try}


class ProfileActor extends Actor {

  val logger = LoggerFactory.getLogger(this.getClass.getName)

  private var profileId = ""

  private var sharedList: Map[String, SharedInfo] = Map.empty[String, SharedInfo]
  private var userList: Map[String, User] = Map.empty[String, User]


  override def preStart(): Unit = {
    logger.info("ProfileActor start ===> " + self)
    profileId = self.path.name

    /**
     * add some logic to load data from the persistent
     * Event Source
     */
  }

  private def map2Seq = {
    sharedList.toSeq.map(_._2)
  }


  def receive = {

    case user@User(userId, userName) => {
      logger.info(s"Add user: userId = ${userId}, userName = ${userName}")
      userList += user.userId -> user
      sender ! user
    }

    case AddShare(share) => {
      val caller = sender()
      Try {
        sharedList += share.shareId -> share
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
