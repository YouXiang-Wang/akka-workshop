package tech.parasol.akka.workshop.http

final case class ApiError(errorCode: String,
                          errorMsg: Option[String]
                       )
