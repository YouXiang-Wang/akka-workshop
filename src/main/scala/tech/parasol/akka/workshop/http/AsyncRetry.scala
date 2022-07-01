package tech.parasol.akka.workshop.http

import java.lang.Math.max

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.TcpIdleTimeoutException
import akka.util.ByteString
import de.heikoseeberger.akkahttpjackson.JacksonSupport._
import org.slf4j.Logger
import tech.parasol.akka.workshop.model._
import tech.parasol.akka.workshop.route.retry._
import tech.parasol.akka.workshop.stream.StreamIteratorFlatMapConcatTest.logger
import tech.parasol.akka.workshop.utils.JsonUtil

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{Manifest, classTag}
import scala.util.{Failure, Try}

trait AsyncRetry {

  implicit val logger: Logger

  def request(request: HttpRequest, requestId: Option[String] = None): Future[HttpResponse]


  def requestWithResponseResult[R: Manifest](req: HttpRequest, requestId: Option[String] = None)
                                            (implicit materializer: Materializer, ec: ExecutionContext): Future[Either[ApiError, ResponseResult[R]]] = {
    request(req, requestId)
      .flatMap(resp => {
        val res = if(resp.status.isSuccess()) {
          extractString(resp).flatMap(r => {
            val result = Try(JsonUtil.convert[ResponseResult[R]](r.get)) match {
              case scala.util.Success(v) => Future.successful(Right(v))
              case Failure(e) => {
                logger.error(s"Exception to convert ${r.get} to Class ${classTag[R].runtimeClass}===> ", e)
                req.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map(s => {
                  Left(ApiError("ExceptionInParsingJson", Option(e.getMessage)))
                })
              }
            }
            result
          })
        } else {
          req.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map(s => {
            val payload = s.utf8String
            val code = resp.status.intValue()
            val error = ApiError(code.toString, Option(s"Exception to call the rest api. code = ${code}, url = [${req.uri}], method = [${req.method}], payload = [${payload}]"))
            Left(error)
          })
        }
        res
      })
  }



  def requestT[R: Manifest](req: HttpRequest, requestId: Option[String] = None)
                           (implicit materializer: Materializer, ec: ExecutionContext): Future[Either[ApiError, R]] = {
    request(req, requestId)
      .flatMap(resp => {
        val res = if(resp.status.isSuccess()) {
          extractString(resp).flatMap(r => {
            val result = Try(JsonUtil.convert[R](r.get)) match {
              case scala.util.Success(v) => Future.successful(Right(v))
              case Failure(e) => {
                logger.error(s"Exception to convert ${r.get} to Class ${classTag[R].runtimeClass}===> ", e)
                req.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map(s => {
                  Left(ApiError("ExceptionInParsingJson", Option(e.getMessage)))
                })
              }
            }
            result
          })
        } else {
          req.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map(s => {
            val payload = s.utf8String
            val code = resp.status.intValue()
            val error = ApiError(code.toString, Option(s"Exception to call the rest api. code = ${code}, url = [${req.uri}], method = [${req.method}], payload = [${payload}]"))
            Left(error)
          })
        }
        res
      })
  }



  def requestLR[L: Manifest, R: Manifest](req: HttpRequest, requestId: Option[String] = None,
                                         f: (HttpRequest, HttpResponse, Option[String], Option[Throwable]) => L)
                                        (implicit materializer: Materializer, ec: ExecutionContext): Future[Either[L, R]] = {
    request(req, requestId)
      .flatMap(resp => {
        val res = if(resp.status.isSuccess()) {
          extractString(resp).flatMap(r => {
            val result = Try(JsonUtil.convert[R](r.get)) match {
              case scala.util.Success(v) => Future.successful(Right(v))
              case Failure(e) => {
                logger.error(s"Exception to convert ${r.get} to Class ${classTag[R].runtimeClass}===> ", e)
                req.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map(s => {
                  val payload = s.utf8String
                  Left(f(req, resp, Option(payload), Option(e)))
                })
              }
            }
            result
          })
        } else {
          req.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map(s => {
            val payload = s.utf8String
            Left(f(req, resp, Option(payload), None))
          })
        }
        res
      })
  }

  private def recoverWhenUnauthorized(in: => Future[HttpResponse])(implicit ec: ExecutionContext): Future[HttpResponse] = {
    implicit val success: Success[HttpResponse] = Success(_.status == StatusCodes.OK)
    When {
      case HttpResponse(StatusCodes.Unauthorized, _, _, _) =>
        logger.info("Request has not been authorized. Refreshing access token and performing a retry")
        Directly(max = 2)
    }(in)
  }

  private def recoverWhenInternalServerError(in: => Future[HttpResponse], retryPolicy: Policy = Backoff(max = 10)(odelay.Timer.default))(implicit ec: ExecutionContext): Future[HttpResponse] = {
    implicit val success: Success[HttpResponse] = Success(_.status == StatusCodes.OK)
    When {
      case HttpResponse(StatusCodes.InternalServerError, _, _, _) =>
        logger.error("recoverWhenInternalServerError ===> There was an internal server error.")
        retryPolicy
    }(in)
  }

  private def recoverWhenNotFound(retryPolicy: Policy = Directly(max = 2))(in: => Future[HttpResponse])(implicit ec: ExecutionContext): Future[HttpResponse] = {
    implicit val success: Success[HttpResponse] = Success(_.status == StatusCodes.OK)
    When {
      case HttpResponse(StatusCodes.NotFound, _, _, _) =>
        logger.info("Request has not been found. Refreshing access token and performing a retry")
        /**
         * Backoff(max = 10)(odelay.Timer.default)
         */
        retryPolicy
    }(in)
  }


  private def recoverWhenRateLimitExceeded(retryPolicy: Policy = Pause(10, 2.seconds))(in: => Future[HttpResponse])(implicit ec: ExecutionContext): Future[HttpResponse] = {
    implicit val success: Success[HttpResponse] = Success(_.status == StatusCodes.OK)
    When {
      case response @ HttpResponse(StatusCodes.TooManyRequests, _, _, _) =>

        /**
         * val rateLimit = RateLimit(response)
         * val until     = Instant.now.until(Instant.ofEpochSecond(rateLimit.reset), ChronoUnit.SECONDS)
         *
         */

        val until = 2
        val pause = max(0, until).seconds
        logger.info(s"Request rate limit exceeded. Waiting $until seconds to retry")
        retryPolicy
    }(in)
  }


  def default(req: HttpRequest, policy: Policy)(implicit ec: ExecutionContext) =
    recoverWhenNotFound(policy) {
      recoverWhenUnauthorized {
        recoverWhenRateLimitExceeded(policy) {
          recoverWhenInternalServerError {
            request(req)
          }
        }
      }
    }

  def async(req: HttpRequest, withRetry: Boolean = true, policy: Policy = Directly(max = 2))(implicit ec: ExecutionContext): Future[HttpResponse] = {
    if(withRetry) {
      recoverWhenNotFound(policy) {
        recoverWhenUnauthorized {
          recoverWhenRateLimitExceeded(policy) {
            recoverWhenInternalServerError {
              request(req)
            }
          }
        }
      }
    } else {
      request(req)
    }
  }

  def asyncF(req: HttpRequest, withRetry: Boolean = true, policy: Policy = Directly(max = 2), f: (HttpRequest, Policy, ExecutionContext) => Future[HttpResponse] = default(_, _)(_))(implicit ec: ExecutionContext): Future[HttpResponse] = {
    if(withRetry) {
      f(req, policy, ec)
    } else {
      request(req)
    }
  }



  def asyncF1(req: HttpRequest, withRetry: Boolean = true, policy: Policy = Directly(max = 2), f: (HttpRequest, Policy, ExecutionContext) => Future[HttpResponse] = default(_, _)(_))(implicit ec: ExecutionContext): Future[HttpResponse] = {
    if(withRetry) {
      f(req, policy, ec)
    } else {
      request(req)
    }
  }

  def asyncWithEither(req: HttpRequest, withRetry: Boolean = false)(implicit materializer: Materializer, ec: ExecutionContext): Future[Either[ApiError, String]] = {
    async(req, withRetry).flatMap(r => extractStringEither(r))
  }



  def asyncWith[T: Manifest](req: HttpRequest, withRetry: Boolean = false)(implicit materializer: Materializer, ec: ExecutionContext): Future[Option[T]] = {
    async(req, withRetry)
      .flatMap(r => extractString(r))
      .map(r => {
        if(r.isDefined) Option(JsonUtil.parseJsonAs[T](r.get)) else None
      })
  }

  def asyncWithString(req: HttpRequest, withRetry: Boolean = false)(implicit materializer: Materializer, ec: ExecutionContext): Future[Option[String]] = {
    async(req, withRetry)
      .flatMap(r => extractString(r))
  }


  def asyncResponseString(req: HttpRequest, withRetry: Boolean = false)(implicit materializer: Materializer, ec: ExecutionContext): Future[Response[String]] = {
    async(req, withRetry)
      .flatMap(r => {
        val res = if(r.status.isSuccess()) {
          extractString(r).map(r => RequestSuccess(result = r.get))
        } else {
          val code = r.status.intValue()
          Future.successful(RequestFailure(error = ApiError(code.toString, None)))
        }
        res
      })
  }

  def asyncRestResultResponse[T: Manifest](req: HttpRequest, withRetry: Boolean = false)(implicit materializer: Materializer, ec: ExecutionContext): Future[Response[T]] = {
    async(req, withRetry)
      .flatMap(r => {
        val res = if(r.status.isSuccess()) {
          extractString(r).map(r => {
            val v = JsonUtil.parseJsonAs[RestResult2[T]](r.get)
            RequestSuccess(result = v.data)
          })
        } else {
          val code = r.status.intValue()
          Future.successful(RequestFailure(error = ApiError(code.toString, None)))
        }
        res
      })
  }



  def recoverWhenTooManyClients[R: Manifest](in: => Future[Either[ApiError, ResponseResult[R]]], retryPolicy: Policy = Pause(10, 1.seconds))(implicit ec: ExecutionContext): Future[Either[ApiError, ResponseResult[R]]] = {
    implicit val success: tech.parasol.akka.workshop.route.retry.Success[ResponseResult[_]] = tech.parasol.akka.workshop.route.retry.Success(_.successful)
    When {
      case Right(ResponseResult(false, responseCode, _, Some("3001"), _)) => {
        logger.info("Too many clients. Retry")
        retryPolicy
      }
      case Left(apiError) => {
        logger.info("apiError ===> " + apiError)
        retryPolicy
      }
      case ResponseResult(false, _, _, _, _) =>
        logger.info("Request has not been found. Refreshing access token and performing a retry")
        retryPolicy

      case Right(res) => {
        logger.info("res ===> " + res)
        retryPolicy
      }
    }(in)
  }

  def asyncR[R: Manifest](req: HttpRequest, withRetry: Boolean = true)(implicit materializer: Materializer, ec: ExecutionContext): Future[Either[ApiError, ResponseResult[R]]] = {
    if(withRetry) {
      recoverWhenTooManyClients {
        requestWithResponseResult(req)
      }
    } else {
      requestWithResponseResult(req)
    }
  }


  def asyncT[T: Manifest](req: HttpRequest, withRetry: Boolean = false)(implicit materializer: Materializer, ec: ExecutionContext): Future[Either[ApiError, T]] = {
    asyncF(req, withRetry)
      .flatMap(resp => {
        val res = if(resp.status.isSuccess()) {
          extractString(resp).map(r => {
            val result = Try(JsonUtil.convert[T](r.get)) match {
              case scala.util.Success(v) => Right(v)
              case Failure(e) => {
                logger.error(s"Exception to convert ${r.get} to Class ===> ", e)
                Left(ApiError("ExceptionInParsingJson", Option(e.getMessage)))
              }
            }
            result
          })
        } else {
          req.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map(s => {
            val payload = s.utf8String
            val code = resp.status.intValue()
            val error = ApiError(code.toString, Option(s"Exception to call the rest api. code = ${code}, url = [${req.uri}], method = [${req.method}], payload = [${payload}]"))
            Left(error)
          })
        }
        res
      }) recover {
      case e: TcpIdleTimeoutException => {
        logger.error(s"asyncT TcpIdleTimeoutException = url = ${req.uri.toString()}, message = ${e.getMessage}")
        val error = ApiError("TcpIdleTimeoutException", Option(e.getMessage))
        Left(error)
      }
      case e: Exception => {
        logger.error(s"UnknownException ===> ${e.getMessage}")
        val error = ApiError("UnknownException", Option(e.getMessage))
        Left(error)
      }
    }
  }


  def asyncResponseResult[T: Manifest](req: HttpRequest, withRetry: Boolean = false)(implicit materializer: Materializer, ec: ExecutionContext): Future[ResponseResult[T]] = {
    async(req, withRetry)
      .flatMap(resp => {
        val res = if(resp.status.isSuccess()) {
          extractString(resp).map(r => {
            val result = Try(JsonUtil.convert[T](r.get)) match {
              case scala.util.Success(v) => {
                ResponseResult[T](
                  successful = true,
                  responseCode = resp.status.intValue(),
                  data = Option(v)
                )
              }
              case Failure(e) => {
                logger.error(s"Exception to convert ${r.get} to Class ===> ", e)
                val code = resp.status.intValue()
                ResponseResult[T](
                  successful = false,
                  responseCode = code,
                  errorCode = Option(code.toString),
                  message = Option(e.getMessage),
                )
              }
            }
            result
          })
        } else {
          val code = resp.status.intValue()
          val result = ResponseResult[T](
            successful = false,
            responseCode = code,
            errorCode = Option(code.toString),
            message = Option(resp.status.value),
          )
          Future.successful(result)
        }
        res
      }) recoverWith(errorHandle)
  }


  def errorHandle[T]: PartialFunction[Throwable, Future[ResponseResult[T]]] = {
    case e: Exception => {
      logger.error("Exception of asyncResponseResult ===> " + e.getMessage)
      val result = ResponseResult[T](
        successful = false,
        responseCode = -1,
        message = Option(e.getCause.getMessage),
      )
      Future.successful(result)
    }
  }

  def asyncWithResponseStringResult(req: HttpRequest, withRetry: Boolean = false)(implicit materializer: Materializer, ec: ExecutionContext): Future[ResponseStringResult] = {
    async(req, withRetry)
      .flatMap(r => extractResponseStringResult(r))
  }


  def extractPayload[T](response: HttpResponse)
                       (implicit materializer: Materializer, ec: ExecutionContext, m: Manifest[T]): Future[Response[T]] = response.status match {
    case StatusCodes.OK => {
      Unmarshal(response).to[T].map(r => {
        RequestSuccess(
          body = None,
          result = r
        )
      })
    }
    case StatusCodes.Unauthorized => Future.failed(new Exception(s"Twitch client unauthorized"))
    case code                     => Future.failed(new Exception(s"Twitch server respond with code $code"))
  }



  def extractString(response: HttpResponse)
                   (implicit materializer: Materializer, ec: ExecutionContext): Future[Option[String]] =
    if(response.status.isSuccess()) {
      response.entity.withoutSizeLimit.dataBytes.runFold(ByteString.empty) {
        case (acc, b) => acc ++ b
      }.map { s =>
        Option(s.utf8String)
      }
    } else {
      logger.error(s"[extractPayloadString] Error in request activity API, code = ${response.status.intValue()}")
      Future.successful(None)
    }



  def extractResponseStringResult(response: HttpResponse)
                                 (implicit materializer: Materializer, ec: ExecutionContext): Future[ResponseStringResult] =
    if(response.status.isSuccess()) {
      response.entity.withoutSizeLimit.dataBytes.runFold(ByteString.empty) {
        case (acc, b) => acc ++ b
      }.map { s =>
        ResponseStringResult(data = Option(s.utf8String))
      }
    } else {
      logger.error(s"[extractResponseStringResult] Error in request activity API, code = ${response.status.intValue()}")
      val resStringResult = ResponseStringResult(
        successful = false,
        responseCode = response.status.intValue(),
        None,
        message = Option(response.status.value))
      Future.successful(resStringResult)
    }

  def extractStringEither(response: HttpResponse)
                           (implicit materializer: Materializer, ec: ExecutionContext): Future[Either[ApiError, String]] =
    if(response.status.isSuccess()) {
      response.entity.withoutSizeLimit.dataBytes.runFold(ByteString.empty) {
        case (acc, b) => acc ++ b
      }.map { s =>
        Right(s.utf8String)
      }
    } else {
      logger.error(s"[extractPayloadString1] Error in request activity API, code = ${response.status.intValue()}")
      val error = ApiError(response.status.intValue().toString, Option(s"Error in request activity API, code = ${response.status.intValue()}"))
      Future.successful(Left(error))
    }

}
