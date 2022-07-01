package tech.parasol.akka.workshop.http

import akka.http.scaladsl.settings.ConnectionPoolSettings

object HttpClientConf {

  val maxTimeoutIdle = 120

  val defaultHttpClientConf =
    s"""
       |akka {
       |  http {
       |    client {
       |      user-agent-header = akka-http
       |      idle-timeout = $maxTimeoutIdle s
       |    }
       |
       |    host-connection-pool {
       |      max-connections = 512
       |      min-connections = 128
       |      max-retries = 5
       |      max-open-requests = 16384
       |      idle-timeout = $maxTimeoutIdle s
       |      pipelining-limit = 1
       |      client {
       |        idle-timeout = $maxTimeoutIdle s
       |      }
       |    }
       |  }
       |  ssl-config {
       |    loose.disableHostnameVerification = false
       |    hostnameVerifierClass = parasol.service.http.client.CustomizedHostnameVerifier
       |  }
       |}
    """.stripMargin

  val defaultConnectionSettings: ConnectionPoolSettings = ConnectionPoolSettings(defaultHttpClientConf)

}
