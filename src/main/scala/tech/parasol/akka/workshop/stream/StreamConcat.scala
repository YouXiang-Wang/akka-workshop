package tech.parasol.akka.workshop.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorAttributes, ActorMaterializer, OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, SourceQueueWithComplete}
import org.slf4j.LoggerFactory
import tech.parasol.akka.workshop.cluster.RawData
import tech.parasol.akka.workshop.http.{ApiError, HttpClient, HttpUtils}
import tech.parasol.akka.workshop.model.{Category, Item, ResponseResult, UserProfile, UserPurchase}
import tech.parasol.akka.workshop.utils.{DefaultExceptionDecider, StreamHelper}

import scala.concurrent.Future

object StreamConcat {

  implicit val system = ActorSystem("StreamConcat")
  implicit val materializer = ActorMaterializer()
  implicit val ex = system.getDispatcher

  lazy val httpClient = HttpClient(system)
  implicit val logger = LoggerFactory.getLogger(this.getClass.getName)

  val parallelism: Int = 4


  def getData(url: String, rawData: RawData) = {
    val request = HttpUtils.buildRequest[RawData](url, "POST", Option(rawData))
    httpClient.asyncT[ResponseResult[Seq[RawData]]](request, true).map(r => r match {
      case Right(v) => {
        if (v.data.isDefined)
          Right(v.data.get)
        else
          Right(Seq.empty)
      }
      case Left(error) => {
        logger.error(s"fetchResourceTypesException ===> ${error.errorMsg}")
        Left(error)
      }
    }) recover {
      case e: Exception => {
        logger.error("fetchResourceTypesException ===> ", e)
        val error = ApiError("RestApiError", Option(e.getMessage))
        Left(error)
      }
    }
  }



  private val commandQueue: Source[RawData, SourceQueueWithComplete[RawData]] =
    Source.queue[RawData](bufferSize = 32768,
      overflowStrategy = OverflowStrategy.backpressure)


  val queue = commandQueue
    .mapAsync(10)(r => {
      getData(s"http://127.0.0.1:18090/rawData/${r.id}", r).map(
        res => res match {
          case Right(v) => {
            v
          }
          case Left(error) => {
            Seq(RawData("", ""))
          }
        })
    })
    .mapAsync(4)(r => {
      Future {
        Thread.sleep(2000)
        println("r ===> " + r)
        (r(0), r(0))
      }
    })
    .toMat(Sink.ignore)(Keep.left)
    .withAttributes(ActorAttributes.supervisionStrategy(DefaultExceptionDecider.decider))
    .run


  private val userQueue: Source[UserPurchase, SourceQueueWithComplete[UserPurchase]] =
    Source.queue[UserPurchase](bufferSize = 32768,
      overflowStrategy = OverflowStrategy.backpressure)

  def getPurchaseData(url: String, category: Category) = {
    val request = HttpUtils.buildRequest[Category](url, "POST", Option(category))
    httpClient.asyncT[ResponseResult[Seq[Item]]](request, true).map(r => r match {
      case Right(v) => {
        if (v.data.isDefined)
          Right(v.data.get)
        else
          Right(Seq.empty)
      }
      case Left(error) => {
        logger.error(s"fetchResourceTypesException ===> ${error.errorMsg}")
        Left(error)
      }
    }) recover {
      case e: Exception => {
        logger.error("fetchResourceTypesException ===> ", e)
        val error = ApiError("RestApiError", Option(e.getMessage))
        Left(error)
      }
    }
  }


  val loadCategory: Flow[UserPurchase, (UserProfile, Category), NotUsed] = Flow[UserPurchase].map(p => {
    p.categories.map(r => (p.user, r))
  })
    .mapConcat(identity)


  val resourceQueryDataFlow = Flow[(UserProfile, Category)]
    .mapAsyncUnordered(10)(x => Future(x))
    .async
    .flatMapConcat(m => {
    val cate = m._2
    val fut = getPurchaseData(s"http://127.0.0.1:18090/purchase/${cate.id}", cate).map(
      res => res match {
        case Right(v) => {
          v
        }
        case Left(error) => {
          Seq(Item("", ""))
        }
      })
    Source.future(fut)
      .mapConcat(r => r)
      .async
  }).async


  val resourceQueryDataFlow1 = Flow[(UserProfile, Category)]
    //.mapAsyncUnordered(parallelism)(x => Future(x))
    //.async
    .mapAsyncUnordered(parallelism)(m => {
      val cate = m._2
      val fut = getPurchaseData(s"http://127.0.0.1:18090/purchase/${cate.id}", cate).map(
        res => res match {
          case Right(v) => {
            v
          }
          case Left(error) => {
            Seq(Item("", ""))
          }
        })

      fut

    }).async
    .mapConcat(identity)


  val queue1 = userQueue
      .via(loadCategory)
    .via(StreamHelper.balancer(resourceQueryDataFlow1, parallelism))
    .map(r => {
      println("item ===> " + r.itemName)
      r
    })
    .toMat(Sink.ignore)(Keep.left)
    .withAttributes(ActorAttributes.supervisionStrategy(DefaultExceptionDecider.decider))
    .run



  def sendUserPurchase(rawData: UserPurchase) = {
    queue1.offer(rawData).map {
      case QueueOfferResult.Enqueued => {
        logger.info(s"[sendUserPurchase] enqueued.")
      }
      case QueueOfferResult.Dropped => {
        logger.info(s"[sendUserPurchase] dropped.")
      }
      case QueueOfferResult.Failure(e) => {
        logger.info(s"[sendUserPurchase] failed.")
      }
      case QueueOfferResult.QueueClosed => {
        logger.info(s"[sendUserPurchase] closed.")
      }
    }
  }


  def sendData(rawData: RawData) = {
    queue.offer(rawData).map {
      case QueueOfferResult.Enqueued => {
        //logger.info(s"[ElasticSearchTest] enqueued.")
      }
      case QueueOfferResult.Dropped => {
        logger.info(s"[ElasticSearchTest] dropped.")
      }
      case QueueOfferResult.Failure(e) => {
        logger.info(s"[ElasticSearchTest] failed.")
      }
      case QueueOfferResult.QueueClosed => {
        logger.info(s"[ElasticSearchTest] closed.")
      }
    }
  }



  def main(args: Array[String]): Unit = {

    val count = 10

    val cates = Seq(
      Category("1", "cate1"),
      Category("2", "cate2"),
      Category("3", "cate3"),
      Category("4", "cate4"),
      Category("5", "cate5"),
      Category("6", "cate6"),
      Category("7", "cate7"),
      Category("8", "cate8")
    )

    val users = Seq(
      UserProfile("u1"),
      UserProfile("u2"),
      UserProfile("u3"),
      UserProfile("u4"),
      UserProfile("u5"),
      UserProfile("u6"),
      UserProfile("u7"),
      UserProfile("u8"),
      UserProfile("u9"),
      UserProfile("u10"),
    )

    (0 to count - 1).map(x => {
      val i = x % 10
      val up = UserPurchase(users(i), cates)
      sendUserPurchase(up)
    })

  }
}

