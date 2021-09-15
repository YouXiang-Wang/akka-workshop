package tech.parasol.akka.workshop.cluster


import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import akka.management.scaladsl.AkkaManagement
import tech.parasol.akka.workshop.route.Route
import tech.parasol.akka.workshop.utils.CommonUtils

object ShardingApp extends Route {

  val logger = LoggerFactory.getLogger(this.getClass.getName)
  val role = "demo"
  val clusterConfig =
    """
      |  akka.remote {
      |    enabled-transports = ["akka.remote.netty.tcp"]
      |    netty.tcp {
      |      hostname = "127.0.0.1"
      |    }
      |  }
      |
      |  akka.cluster {
      |    seed-nodes = [
      |      "akka://ShardingApp@127.0.0.1:2661"
      |    ]
      |
      |    // Needed to move the cluster-shard to another node
      |    // Do not in production
      |    // auto-down-unreachable-after = 3s
      |  }
      |
      |
      |""".stripMargin

  val port = s"${CommonUtils.availablePort(Seq(2661, 2662, 2663, 2664, 2665))}"

  val config = ConfigFactory.parseString(s"akka.cluster.roles = [${role}]")
    .withFallback(ConfigFactory.parseString("akka.log-config-on-start = on"))
    .withFallback(ConfigFactory.parseString(s"akka.remote.artery.canonical.port = ${port}"))
    .withFallback(ConfigFactory.parseString(s"akka.http.server.request-timeout = 60 s"))
    .withFallback(ConfigFactory.parseString(clusterConfig))
    .withFallback(ConfigFactory.load())

  implicit val system = ActorSystem("ShardingApp", config)
  implicit val executionContext = system.dispatcher

  Application.system = system

  def startCluster = {
    val bind = CommonUtils.availablePort(Seq(18090, 18091, 18092, 18093, 18094))
    Http().bindAndHandle(router, "0.0.0.0", bind)
    AkkaManagement(system).start()
    logger.info(s"ShardingApp starts up at ${bind}")
    Application.shardingRegion = ShardingHelper.startShardingRegion(system, "user")
    Application.profileShardingRegion = ShardingHelper.startProfileShardingRegion(system, "profile")
    CoordinatedShutdown(system).addJvmShutdownHook {
      logger.info(s"[ShardingApp] shutdown at ${System.currentTimeMillis()}")
    }
  }

  /**
   *
   * curl http://127.0.0.1:8558/cluster/members
   *
   *
   * curl -X POST 'http://127.0.0.1:18090/user/user_1' -H "Content-type:application/json" -d  '{"userId":"user_1","userName":"name_1"}'
   * curl -X POST 'http://127.0.0.1:18090/user/user_2' -H "Content-type:application/json" -d  '{"userId":"user_2","userName":"name_2"}'
   *
   * curl -X POST 'http://127.0.0.1:18090/profile/profile_1' -H "Content-type:application/json" -d  '{"userId":"user_1","userName":"name_1"}'
   * curl -X POST 'http://127.0.0.1:18090/profile/profile_2' -H "Content-type:application/json" -d  '{"userId":"user_1","userName":"name_1"}'
   *
   * curl -X POST http://127.0.0.1:18090/profile/profile_1/share -H "Content-type:application/json" -d  '{"profileId":"profile_1","shareId":"share_1_1"}'
   * curl -X POST http://127.0.0.1:18090/profile/profile_1/share -H "Content-type:application/json" -d  '{"profileId":"profile_1","shareId":"share_1_2"}'
   * curl -X POST http://127.0.0.1:18090/profile/profile_2/share -H "Content-type:application/json" -d  '{"profileId":"profile_2","shareId":"share_2_1"}'
   *
   * curl http://127.0.0.1:18090/profile/profile_1
   * curl http://127.0.0.1:18090/profile/profile_2
   *
   *
   *
   */

  def main(args: Array[String]): Unit = {
    startCluster
  }

}
