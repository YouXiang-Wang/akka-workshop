package tech.parasol.akka.workshop.http

import java.io.{IOException, InputStream}
import java.net.{Socket, URLEncoder}

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.{handleRejections, mapResponse}
import akka.http.scaladsl.server.{RejectionHandler, Route}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import tech.parasol.akka.workshop.utils.JsonUtil

import scala.annotation.{tailrec, varargs}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

object HttpUtils {

  implicit val logger = LoggerFactory.getLogger(this.getClass.getName)

  val urlPathSeparator = "/"

  val urlPathSeparatorChar = '/'

  val AuthorizationBearer = "Authorization"

  val NotFound = HttpResponse(StatusCodes.NotFound)

  val BadRequest = HttpResponse(StatusCodes.BadRequest)

  val Unauthorized = HttpResponse(StatusCodes.Unauthorized)

  val ActionSucceed: (String) => HttpResponse = {
    action =>
      HttpResponse(status = StatusCodes.OK, entity = HttpEntity.Strict(ContentTypes.`application/json`, ByteString(s"""{"${action}":"successful"}""")))
  }

  val RawUnauthorizedRes: (String) => HttpResponse = {
    message =>
      HttpResponse(status = StatusCodes.Unauthorized, entity = HttpEntity.Strict(ContentTypes.`application/json`, ByteString(message)))
  }


  val RawUnauthorizedResHeader: (String) => HttpResponse = {
    message => {
      val header = RawHeader("X-401-Reason", message)
      val headers = List(header)
      HttpResponse(status = StatusCodes.Unauthorized, headers = headers,
        entity = HttpEntity.Strict(ContentTypes.`application/json`, ByteString(message)))
    }
  }

  val InternalServerError = HttpResponse(StatusCodes.InternalServerError)


  val NotFoundRes = HttpResponse(status = StatusCodes.NotFound,
    entity = HttpEntity.Strict(ContentTypes.`application/json`, ByteString(ExceptionResources.ResourceIsNotFound)))


  val UnauthorizedRes = HttpResponse(status = StatusCodes.Unauthorized,
    entity = HttpEntity.Strict(ContentTypes.`application/json`, ByteString(ExceptionResources.AuthenticationFailure)))

  val TokenIsExpiredRes = HttpResponse(status = StatusCodes.Unauthorized,
    entity = HttpEntity.Strict(ContentTypes.`application/json`, ByteString(ExceptionResources.TokenIsExpired)))


  val ErrorPayloadRes = HttpResponse(status = StatusCodes.Unauthorized,
    entity = HttpEntity.Strict(ContentTypes.`application/json`, ByteString(ExceptionResources.ErrorPayload)))


  val InternalServerErrorRes = HttpResponse(status = StatusCodes.InternalServerError,
    entity = HttpEntity.Strict(ContentTypes.`application/json`, ByteString(ExceptionResources.InternalServerError)))


  val ServiceUnavailableErrorRes = HttpResponse(status = StatusCodes.ServiceUnavailable,
    entity = HttpEntity.Strict(ContentTypes.`application/json`, ByteString(ExceptionResources.ServiceUnavailableError)))


  def traceRequest(inner: Route)(implicit ec: ExecutionContext): Route = { ctx =>

    val rejectionHandler = RejectionHandler.default

    val start = System.currentTimeMillis()

    val innerRejectionsHandled = handleRejections(rejectionHandler)(inner)

    mapResponse { resp =>
      val currentTime = new DateTime()
      val currentTimeStr = currentTime.toString
      val duration = System.currentTimeMillis() - start
      var remoteAddress = ""
      var userAgent = ""
      var rawUri = ""
      ctx.request.headers.foreach(header => {
        // this setting come from nginx
        if (header.name() == "X-Real-Ip") {
          remoteAddress = header.value()
        }
        if (header.name() == "User-Agent") {
          userAgent = header.value()
        }
        // you must set akka.http.raw-request-uri-header=on config
        if (header.name() == "Raw-Request-URI") {
          rawUri = header.value()
        }
      })

      Future {
        val mapPattern = Seq("chat", "upload")
        var isIgnore = false
        mapPattern.foreach(pattern =>
          isIgnore = isIgnore || rawUri.startsWith(s"/$pattern")
        )
        if (!isIgnore) {
          println(s"# $currentTimeStr ${ctx.request.uri} [$remoteAddress] [${ctx.request.method.name}] [${resp.status.value}] [$userAgent] took: ${duration}ms")
        }
      }
      resp
    }(innerRejectionsHandled)(ctx)
  }

  def logRoute(implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer, notificationActor: ActorRef, route: Route) = traceRequest(route)

  @inline
  def tweakUrl(address: String, path: String) = {
    val goodPath = if (path.startsWith("/")) path.substring(1) else path
    s"${address}/${goodPath}"
  }


  @inline
  def combineUrls(segments: String*) = {
    val rawUrl = segments.toArray.filterNot(_ == "").mkString(urlPathSeparator)
    val url = rawUrl.replaceAll(s"/{2,}", urlPathSeparator)
    logger.info(s"API provided by gateway -> ${url}")
    url
  }


  @inline
  def combineUrlsWithNoLog(segments: String*) = {
    val rawUrl = segments.toArray.filterNot(_ == "").mkString(urlPathSeparator)
    val url = rawUrl.replaceAll(s"/{2,}", urlPathSeparator)
    url
  }


  @inline
  def extractIpFromHeader(headers: Seq[HttpHeader]): Option[HttpHeader] = {
    headers.find(_.is("x-forwarded-for")).orElse(headers.find(_.is("remote-address"))).orElse(headers.find(_.is("x-real-ip")))
  }

  @inline
  def extractHeaderValue(headers: Seq[HttpHeader], headerKey: String): Option[String] = {
    headers.find(_.is(headerKey.toLowerCase())) match {
      case Some(header) => Some(header.value())
      case _ => None
    }

  }

  @inline
  def extractIpAddress(headers: Seq[HttpHeader]): String = {
    val ipStr = extractIpFromHeader(headers).map { header =>
      val name = header.name()
      name.toLowerCase match {
        case "x-forwarded-for" => {
          header.value().split(",")(0)
        }
        case "remote-address" => header.value()
        case "x-real-ip" => header.value()
        case _ => ""
      }
    }
    val str = ipStr.get.split(":")(0)
    if (str == "0") "0:0:0:0:0:0:0:1" else str
  }


  @inline
  def extractUserAgentWithHash(headers: Seq[HttpHeader]): String = {
    headers.find(_.is("user-agent")) match {
      case Some(h) => h.value().hashCode.toString
      case _ => "no-user-agent"
    }
  }

  @inline
  def buildEncodedQueryString(params: Map[String, Any]): String = {
    val encoded = for {
      (name, value) <- params if value != None
      encodedValue = value match {
        case Some(x: String) => URLEncoder.encode(x, "UTF-8")
        case x: Int => URLEncoder.encode(x.toString, "UTF-8")
        case x => URLEncoder.encode(x.toString, "UTF-8")
      }
    } yield name + "=" + encodedValue
    encoded.mkString("", "&", "")
  }

  def findTokenFromRequest(request: HttpRequest): Option[String] = findAccessFromHeader(request.headers)

  @inline
  def findAccessFromHeader(headers: Seq[HttpHeader]): Option[String] = headers
    .find(_.is("authorization"))
    .map(_.value())
    .filter(_.startsWith("Bearer"))
    .map(_.drop(7))


  @varargs
  @inline
  def generateIdByParams(params: String*): String = params.mkString("-")

  @inline
  def overwriteMatchedUri(reqUriPattern: String, oriUriPattern: String, tarUriPattern: String): String = {
    /**
     * For example:
     * reqUriPattern: /console/parser
     * oriUriPattern: /console/*.*/*.*
     * tarUriPattern: /console1
     *
     * return /console1/parser
     **/

    oriUriPattern match {

      case "/" => {
        tweakUrl(tarUriPattern, reqUriPattern)
      }

      case "" => {
        ""
      }
      case _ =>
    }

    ""
  }

  def isPortListening(hostName: String, portNumber: Int) = try {
    new Socket(hostName, portNumber).close()
    true
  } catch {
    case _: Exception =>
      false
  }

  def withInterfacePort(authority: String, isSSL: Boolean) = {
    authority.split(":") match {
      case Array(x) => {
        if (isSSL) (x, 443) else (x, 80)
      }
      case Array(x, y) => (x, y.toInt)
    }
  }


  final lazy val API_NOT_FOUND = "The requested resource could not be found."

  def paramsGetString(params: Map[String, String], key: String, default: String = ""): String = {
    var ret = default
    if (params.contains(key)) {
      ret = params(key)
    }
    ret
  }


  def response2Str(response: Future[HttpResponse])(implicit materializer: Materializer, executionContext: ExecutionContext) = {
    response.flatMap(res => {
      res.status.intValue() match {
        case 200 => {
          res.entity.withoutSizeLimit.dataBytes.runFold(ByteString.empty) {
            case (acc, b) => acc ++ b
          }.map { s =>
            val str = s.utf8String
            str
          }
        }
        case _ => Future("")
      }
    })

  }


  def toFormUrlencodedContentType = {
    //ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`)
    ContentType(MediaTypes.`application/x-www-form-urlencoded`, null)
    //val contentType = MediaType("text/html")
    //val contentType = `Content-Type`(MediaType.`application/json`)
  }


  def toHttpMethod(name: String): HttpMethod = name.toUpperCase match {
    case "GET" => HttpMethods.GET
    case "POST" => HttpMethods.POST
    case "PUT" => HttpMethods.PUT
    case "DELETE" => HttpMethods.DELETE
    case "HEAD" => HttpMethods.HEAD
    case "OPTIONS" => HttpMethods.OPTIONS
    case "PATCH" => HttpMethods.PATCH
    case "ALL" => HttpMethod.custom("ALL")
    case "*" => HttpMethod.custom("*")
  }

  def toContentType(ct: Option[String]) = ct match {
    case None => Some(ContentTypes.NoContentType)
    case Some(x) => ContentType.parse(x) match {
      case Right(r) => Some(r)
      case Left(errors) => {
        val msg = errors.map(_.formatPretty).mkString("\n")
        throw new IllegalArgumentException("Malformed Content-Type: \n" + msg)
        None
      }
    }
  }

  val CHARSET_UTF_8 = "UTF-8"
  val CONTENT_TYPE_FORM_ENCODED = "application/x-www-form-urlencoded"
  val CONTENT_TYPE_JSON = "application/json"

  def toHttpHeader(headers: Seq[(String, String)]): Seq[RawHeader] = {
    headers.map(h => RawHeader(h._1, h._2))
  }


  def toHttpEntity(inputStream: InputStream, contentType: Option[ContentType], contentLength: Int): RequestEntity = {
    @tailrec
    def drainRequestInputStream(buf: Array[Byte], inputStream: InputStream, bytesRead: Int = 0): Array[Byte] =
      if (bytesRead < contentLength) {
        val count = inputStream.read(buf, bytesRead, contentLength - bytesRead)
        if (count >= 0) drainRequestInputStream(buf, inputStream, bytesRead + count)
        else throw new Exception("Illegal request entity, " +
          "expected length " + contentLength + " but only has length " + bytesRead)
      } else buf

    val body =
      if (contentLength > 0) {
        if (contentLength <= 100000000000L) {
          try drainRequestInputStream(new Array[Byte](contentLength), inputStream)
          catch {
            case e: IOException =>
              throw new IllegalRequestException(new ErrorInfo("Could not read request entity"), null)
          }
        } else throw new IllegalRequestException(new ErrorInfo("HTTP message Content-Length " +
          contentLength + " exceeds the configured limit of maxContentLength"), StatusCodes.RequestEntityTooLarge)
      } else Array.empty[Byte]
    if (contentType.isEmpty) HttpEntity(body) else HttpEntity(contentType.get, body)
  }



  def buildRequest[T: ClassTag](uri: String, method: String = "POST", data: Option[T] = None): HttpRequest = {
    val entity = if(data.isDefined) {
      val entityStr = JsonUtil.toJson(data.get, false)
      HttpEntity(
        ContentTypes.`application/json`,
        ByteString(entityStr))
    } else HttpEntity.Empty

    val request = HttpRequest(
      method = toHttpMethod(method),
      uri = uri,
      entity = entity
    )

    request
  }

  def buildRequest4String(uri: String, method: String = "POST", data: Option[String] = None): HttpRequest = {
    val entity = if(data.isDefined) {
      HttpEntity(
        ContentTypes.`application/json`,
        ByteString(data.get))
    } else HttpEntity.Empty

    val request = HttpRequest(
      method = toHttpMethod(method),
      uri = uri,
      entity = entity
    )

    request
  }

}
