package tech.parasol.akka.workshop.cluster

import akka.actor.{ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import tech.parasol.akka.workshop.actor.JavaProfileActor

object ShardingHelper {

  def startShardingRegion(system: ActorSystem, role: String) = {
    ClusterSharding(system).start(
      typeName = role,
      entityProps = Props(classOf[JavaProfileActor]),
      settings = ClusterShardingSettings(system),
      extractEntityId = ShardingConfig.extractEntityId,
      extractShardId = ShardingConfig.extractShardId
    )
  }

  def startProfileShardingRegion(system: ActorSystem, role: String) = {
    ClusterSharding(system).start(
      typeName = role,
      entityProps = Props(classOf[ProfileActor]),
      settings = ClusterShardingSettings(system),
      extractEntityId = ShardingConfig.extractEntityId,
      extractShardId = ShardingConfig.extractShardId
    )
  }
}
