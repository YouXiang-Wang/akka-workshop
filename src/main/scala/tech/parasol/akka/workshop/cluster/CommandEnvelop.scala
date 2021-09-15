package tech.parasol.akka.workshop.cluster

final case class CommandEnvelop(id: String, payload: Any)