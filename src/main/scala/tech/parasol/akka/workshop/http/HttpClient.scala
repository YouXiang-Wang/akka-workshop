package tech.parasol.akka.workshop.http


import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.{Http, HttpExt}
import org.slf4j.LoggerFactory

import scala.concurrent.Future

class HttpClient(
                  system: ActorSystem,
                  connectionSettings: ConnectionPoolSettings = HttpClientConf.defaultConnectionSettings
                ) extends AsyncRetry {

  implicit val logger = LoggerFactory.getLogger(this.getClass.getName)

  implicit val http: HttpExt = Http()(system)

  def request(request: HttpRequest, requestId: Option[String] = None): Future[HttpResponse] = {
    http.singleRequest(request)
  }
}

object HttpClient {

  def apply(system: ActorSystem,
            connectionSettings: ConnectionPoolSettings
           ): HttpClient = new HttpClient(system, connectionSettings)


  def apply(system: ActorSystem
           ): HttpClient = new HttpClient(system)

}