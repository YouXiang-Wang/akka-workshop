package tech.parasol.akka.workshop.route


import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import de.heikoseeberger.akkahttpjackson.JacksonSupport._
import tech.parasol.akka.workshop.cluster._
import tech.parasol.akka.workshop.model.{Category, Item, ResponseResult}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
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
      entity(as[SharedInfo]) (sharedInfo => {
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
      })
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


  val rawData = path("rawData" / Segment) { id => {
    post {
      entity(as[RawData]) { rawData => {
        val fut = Future {
          Thread.sleep(10000)
          ResponseResult(
            data = Option(Seq(rawData))
          )
        }

        onComplete(fut) {
          case Success(v) => complete(v)
          case Failure(e) => {
            e.printStackTrace()
            complete("failed")
          }
        }
      }}
    }
  }}

  val purchase = path("purchase" / Segment) { id => {
    post {
      entity(as[Category]) { category => {
        val fut = Future {
          if(category.id == "1") {
            Thread.sleep(10000)
            val item = Item(id = category.id, itemName =  s"${category.id}_sleeping")
            ResponseResult(
              data = Option(Seq(item))
            )
          } else if(category.id == "5") {
            Thread.sleep(5000)
            val item = Item(id = category.id, itemName =  s"${category.id}_sleeping")
            ResponseResult(
              data =  Option(Seq(item))
            )
          } else if(category.id == "7") {
            Thread.sleep(7000)
            val item = Item(id = category.id, itemName =  s"${category.id}_sleeping")
            ResponseResult(
              data =  Option(Seq(item))
            )
          } else {
            val item = Item(id = category.id, itemName =  s"${category.id}_now")
            ResponseResult(
              data = Option(Seq(item))
            )
          }
        }

        onComplete(fut) {
          case Success(v) => complete(v)
          case Failure(e) => {
            e.printStackTrace()
            complete("failed")
          }
        }
      }}
    }
  }}


  val purchaseOrder = path("purchaseOrder" / Segment) { id => {
    post {
      entity(as[Category]) { category => {
        val fut = Future {
          if(category.order == 1) {
            Thread.sleep(10000)
            val item = Item(id = category.id, itemName =  s"${category.id}_sleeping")
            ResponseResult(
              data = Option(Seq(item))
            )
          } else if(category.order == 8) {
            ResponseResult(
              data = Option(Seq.empty)
            )
          } else {
            val item = Item(id = category.id, itemName =  s"${category.id}_now")
            ResponseResult(
              data = Option(Seq(item))
            )
          }
        }

        onComplete(fut) {
          case Success(v) => {
            v.data.map(x => x.headOption.map(r => println(s"onComplete ====> ${r.itemName}")))
            complete(v)
          }
          case Failure(e) => {
            e.printStackTrace()
            complete("failed")
          }
        }
      }}
    }
  }}


  val bulk = path("_bulk") { {
    post {
      complete(StatusCodes.TooManyRequests)
    }
  }}


  val router = welcome ~ user ~ profile ~ addShare ~ getProfile ~ bulk ~ rawData ~ purchase ~ purchaseOrder

}
