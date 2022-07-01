package tech.parasol.akka.workshop.model

import tech.parasol.akka.workshop.http.ApiError


sealed trait Response[+U] {

  def status: Int

  def body: Option[String]

  def headers: Map[String, Seq[String]] = Map.empty

  def result: U

  def error: ApiError

  def isError: Boolean

  final def isSuccess: Boolean = !isError

  def map[V](f: U => V): Response[V]

  def flatMap[V](f: U => Response[V]): Response[V]

  final def fold[V](ifError: => V)(f: U => V): V = if (isError) ifError else f(result)

  final def fold[V](onError: RequestFailure => V, onSuccess: U => V): V = this match {
    case failure: RequestFailure => onError(failure)
    case RequestSuccess(_, _, _, result) => onSuccess(result)
  }

  final def foreach[V](f: U => V): Unit = if (!isError) f(result)

  final def toOption: Option[U] = if (isError) None else Some(result)

  final def toEither: Either[ApiError, U] = if (isError) Left(error) else Right(result)

}


final case class RequestSuccess[U](status: Int = 1,
                                   override val body: Option[String] = None,
                                   override val headers: Map[String, Seq[String]] = Map.empty,
                                   override val result: U
                                  ) extends Response[U] {

  override def isError = false

  override def error = throw new NoSuchElementException(s"Request success $result")

  final def map[V](f: U => V): Response[V] = RequestSuccess(status, body, headers, f(result))

  final def flatMap[V](f: U => Response[V]): Response[V] = f(result)

}

final  case class RequestFailure(status: Int = -1,
                                 override val body: Option[String] = None,
                                 override val headers: Map[String, Seq[String]] = Map.empty,
                                 override val error: ApiError) extends Response[Nothing] {

  override def result = throw new NoSuchElementException(s"Request Failure $error")

  override def isError = true

  final def map[V](f: Nothing => V): Response[V] = this

  final def flatMap[V](f: Nothing => Response[V]): Response[V] = this
}