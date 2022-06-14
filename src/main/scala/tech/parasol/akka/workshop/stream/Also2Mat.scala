package tech.parasol.akka.workshop.stream


import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object Also2Mat extends App {

  private implicit val sys: ActorSystem = ActorSystem()
  private implicit val mat: ActorMaterializer = ActorMaterializer()
  private implicit val ec: ExecutionContext = sys.dispatcher

  val source: Source[Int, Any] = Source((1 to 5).toList)

  def magic(source: Source[Int, Any]): Source[Int, Future[Int]] =
    source.alsoToMat(Sink.fold(0)((acc, _) => acc + 1))((_, f) => f)

  val f = magic(source)
    //.throttle(1, 1.second)
    .map(x => x)
    .alsoToMat(Sink.foreach( element => println("res ===> " + element)))(Keep.left)
    .alsoToMat(Sink.foreach( element => println("res1 ===> " + element)))(Keep.left)
    .toMat(Sink.foreach(println))(Keep.left).run()

  //f.onComplete(t => println(s"f1 completed - $t"))
  //f.onComplete(t => println(s"f1 completed - $t"))
  //Await.ready(f, 5.minutes)


  mat.shutdown()
  sys.terminate()
}