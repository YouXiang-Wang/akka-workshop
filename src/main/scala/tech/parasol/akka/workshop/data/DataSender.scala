package tech.parasol.akka.workshop.data

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import org.slf4j.LoggerFactory
import tech.parasol.akka.workshop.utils.JsonUtil


object DataSender {

  implicit val system = ActorSystem("DataSender")

  implicit val executionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))
  implicit val logger = LoggerFactory.getLogger(this.getClass)


  def postData(index: Int, indexName: String) = {

    val url = "http://127.0.0.1:18070/data/api/data"


    val id = s"id0000${index}"
    val testData = TestData(id = id, data = s"id0000${index}")

    val esEntity = ElasticJsonDataEntity(
      id = Some(id),
      index = indexName,
      data = Option(testData)
    )



    //println(s"${JsonUtil.toJson(Seq(esEntity))}")
    val entity1 = HttpEntity(ContentTypes.`application/json`, JsonUtil.toJson(Seq(esEntity)))


    val request = HttpRequest(method = HttpMethods.POST,
      uri = url,
      entity = entity1
    )

    Http().singleRequest(request)

  }


  def main(args: Array[String]): Unit = {

    val parallelism = 512
    val count = 50000
    val indexName = "keyword_limit_resource.2022.08"
    val b = System.currentTimeMillis()

    Source(1 to count).mapAsyncUnordered(parallelism)( x => postData(x, indexName))
      .runWith(Sink.ignore)
      .onComplete(_ => {
        println("time ===> " + (System.currentTimeMillis() - b))
        system.terminate()
      })


  }
}
