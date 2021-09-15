package tech.parasol.akka.workshop.cluster

import akka.actor.{ActorRef, ActorSystem}

object Application {

  final implicit var system: ActorSystem = _

  var shardingRegion: ActorRef = _

  var profileShardingRegion: ActorRef = _



}
