package tech.parasol.akka.workshop.route


import akka.actor.ActorSystem
import akka.cluster.pubsub.DistributedPubSub
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import de.heikoseeberger.akkahttpjackson.JacksonSupport._
import tech.parasol.akka.workshop.cluster.{Application, CommandEnvelop, ProfileAction, ProfileInfo, SharedInfo, User}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait Route {

  implicit val system: ActorSystem
  implicit val executionContext: ExecutionContext
  implicit val timeout: Timeout = 5 seconds

  val welcome = path("welcome") {
    get {
      complete("Welcome to akka cluster!")
    }
  }

  val user = path("user" / Segment) { userId => {
    post {
      entity(as[User]) { user => {
        val s = (Application.shardingRegion ? CommandEnvelop(user.userId, user)).mapTo[User]
        onComplete(s) {
          case Success(v) => complete(v)
          case Failure(e) => {
            e.printStackTrace()
            complete("failed")
          }
        }
      }}
    }
  }}

  val profile = path("profile" / Segment) { profileId => {
    post {
      entity(as[User]) { user => {
        val s = (Application.profileShardingRegion ? CommandEnvelop(profileId, user)).mapTo[User]
        onComplete(s) {
          case Success(v) => complete(v)
          case Failure(e) => {
            e.printStackTrace()
            complete("failed")
          }
        }
      }}
    }
  }}

  val addShare = path("profile" / Segment / "share") { profileId =>
    post {
      entity(as[SharedInfo]) { sharedInfo => {
        val command = CommandEnvelop(sharedInfo.profileId, ProfileAction.AddShare(sharedInfo))
        val s = (Application.profileShardingRegion ? command).mapTo[Boolean]
        onComplete(s) {
          case Success(v) => {
            complete(v)
          }
          case Failure(e) => {
            e.printStackTrace()
            complete("failed")
          }
        }
      }
      }
    }
  }

  val getProfile = path("profile" / Segment) { profileId => {
    get {
      val command = CommandEnvelop(profileId, ProfileAction.GetProfile(profileId))
      val profileInfoFut = (Application.profileShardingRegion ? command).mapTo[ProfileInfo]
      onComplete(profileInfoFut) {
        case Success(v) => {
          complete(v)
        }
        case Failure(e) => {
          e.printStackTrace()
          complete("failed")
        }
      }
    }
  }}


  val router = welcome ~ user ~ profile ~ addShare ~ getProfile

}
