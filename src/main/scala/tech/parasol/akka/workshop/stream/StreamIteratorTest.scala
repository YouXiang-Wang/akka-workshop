package tech.parasol.akka.workshop.stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import scala.concurrent.duration._
import scala.concurrent.Future

class ResourceDataQueryExecutor(initValue: Int, divisor: Int) {
  var _currentValue: Int = initValue
  def getData: Future[Seq[Int]] = {
    _currentValue = _currentValue / divisor
    Future.successful(Seq(_currentValue))
  }
}

object StreamIteratorTest {

  implicit val system = ActorSystem("StreamIteratorTest")

  implicit val ec = system.dispatcher

  def test = {

    def f: Future[List[Int]] = Future.successful((1 to 5).toList)

    def g(l: List[Int]): List[String] = l.map(_.toString * 2)

    Source
      .future(f)
      .mapConcat(g) // emits 5 elements of type Int
      .runForeach(println)

  }

  def iteratorTest = {

    val t = new ResourceDataQueryExecutor(100, 2)
    val s = Source.fromIterator(() => Iterator.continually(t.getData))
      .throttle(10, 10 seconds)
      .mapAsync(1)(identity)
      .takeWhile(r => {
        if(r(0) > 2)
          true
        else
          false
      }, false)
      .mapConcat(a => {
        val flat = a.map(r => r + 1)
        flat
      })

    s.runForeach(x => println(x))

  }


  def main(args: Array[String]): Unit = {
    iteratorTest
  }

}
