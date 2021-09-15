package tech.parasol.akka.workshop.cluster

import akka.cluster.sharding.ShardRegion

object ShardingConfig {

  private val numberOfShards = 100

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case CommandEnvelop(id, payload) => (id, payload)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case CommandEnvelop(id, _) => (id.hashCode % numberOfShards).toString
    case ShardRegion.StartEntity(id) => (id.hashCode % numberOfShards).toString
  }
}

