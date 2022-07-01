package tech.parasol.akka.workshop.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, SourceQueueWithComplete, TcpIdleTimeoutException}
import akka.stream._
import org.slf4j.LoggerFactory
import tech.parasol.akka.workshop.http.{ApiError, HttpClient, HttpUtils}
import tech.parasol.akka.workshop.model._
import tech.parasol.akka.workshop.stream.StreamIteratorFlatMapConcatTest.logger
import tech.parasol.akka.workshop.utils.{DefaultExceptionDecider, StreamHelper}

import scala.concurrent.{ExecutionContextExecutor, Future}

class OrderQueryExecutor(httpClient: HttpClient, userProfile: UserProfile, category: Category)
                               (implicit materializer: Materializer, executionContext: ExecutionContextExecutor) {
  var _order: Int = 0
  var _cate = category

  private def getPurchaseData(category: Category) = {
    val url = s"http://127.0.0.1:18090/purchaseOrder/${category.id}"
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
      case e: TcpIdleTimeoutException => {
        logger.error("TcpIdleTimeoutException ===> ", e)
        val error = ApiError("RestApiError", Option(e.getMessage))
        Left(error)
      }
      case e: Exception => {
        logger.error("OrderQueryExecutorFetchResourceTypesException ===> ", e)
        val error = ApiError("RestApiError", Option(e.getMessage))
        Left(error)
      }
    }
  }


  def next: Future[Seq[Item]] = {
    _order = _order + 1
    _cate = _cate.copy(order = _order)
    getPurchaseData(_cate).map(r => {
      r match {
        case Right(v) => {
          v
        }
        case Left(e) => Seq.empty
      }
    })
  }
}

object StreamIteratorFlatMapConcatTest {

  implicit val system = ActorSystem("StreamIteratorFlatMapConcatTest")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.getDispatcher

  lazy val httpClient = HttpClient(system)
  implicit val logger = LoggerFactory.getLogger(this.getClass.getName)

  val parallelism: Int = 1

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




  val resourceQueryDataFlow0 = Flow[(UserProfile, Category)]
    .flatMapConcat{ message =>
      val user = message._1
      val cate = message._2
      println("process ===> " + cate.id)
      val queryExecutor = new OrderQueryExecutor(httpClient, user, cate)
      Source.fromIterator(() => Iterator.continually(queryExecutor.next))
        .mapAsync(1)(identity)
        .takeWhile(r => {
          r.size > 0
        }, true)
    }
    .async
    .mapConcat(identity)


  val resourceQueryDataFlow = Flow[(UserProfile, Category)]
    .flatMapMerge(16, { message =>
      val user = message._1
      val cate = message._2
      println("process ===> " + cate.id)
      val queryExecutor = new OrderQueryExecutor(httpClient, user, cate)
      Source.fromIterator(() => Iterator.continually(queryExecutor.next))
        .mapAsync(1)(identity)
        .takeWhile(r => {
          r.size > 0
        }, true)
    })
    .async
    .mapConcat(identity)


  val resourceQueryDataFlow1 = Flow[(UserProfile, Category)]
    .mapAsyncUnordered(parallelism)(m => {
      val cate = m._2
      println("process ===> " + cate.id)
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
    //.buffer(16, OverflowStrategy.backpressure)
    .mapConcat(identity)


  val queue1 = userQueue
      .via(loadCategory)
    .via(StreamHelper.balancer(resourceQueryDataFlow, parallelism))
    //.via(resourceQueryDataFlow1)
    .map(r => {
      println("item ===> " + r.itemName)
      r
    })
    .async
    .toMat(Sink.ignore)(Keep.left)
    .withAttributes(ActorAttributes.supervisionStrategy(DefaultExceptionDecider.decider))
    .run



  def sendUserPurchase(rawData: UserPurchase) = {
    queue1.offer(rawData).map {
      case QueueOfferResult.Enqueued => {
        //logger.info(s"[sendUserPurchase] enqueued.")
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



  def main(args: Array[String]): Unit = {

    val count = 2

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

    val cates2 = Seq(
      Category("1", "cate1"),
      Category("5", "cate5"),
      Category("7", "cate7"),
      Category("8", "cate8"),
      Category("9", "cate9")
    )

    val cates1 = Seq(
      Category("1", "cate1")
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
      val up = UserPurchase(users(i), cates2)
      sendUserPurchase(up)
    })

  }
}

