package tech.parasol.akka.workshop.stream

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Status}
import akka.stream.{ActorMaterializer, CompletionStrategy, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Source}
import cats.instances.queue

import scala.concurrent.Promise

object StatefulMapConcatExample {

  implicit val system = ActorSystem("StatefulMapConcatExample")
  implicit val materializer = ActorMaterializer()


  def test1 = {


    val source = Source(List(
      "a", ">>", "b", "c", "<<", "d", "e", ">>", "f", "g", "h", "<<", "i", ">>", "j", "<<", "k"
    ))

    val extractFlow = Flow[String].statefulMapConcat { () =>
      val start = ">>"
      val stop = "<<"
      var discard = true
      elem =>
        if (discard) {
          if (elem == start)
            discard = false
          Nil
        }
        else {
          if (elem == stop) {
            discard = true
            Nil
          }
          else
            elem :: Nil
        }
    }

    source.via(extractFlow).runForeach(x => println(s"$x "))
    // b c f g h j

  }

  def popFirstMatch(ls: List[Int], condF: Int => Boolean): (Option[Int], List[Int]) = {
    ls.find(condF) match {
      case None =>
        (None, ls)
      case Some(e) =>
        val idx = ls.indexOf(e)
        if (idx < 0)
          (None, ls)
        else {
          val (l, r) = ls.splitAt(idx)
          (r.headOption, l ++ r.tail)
        }
    }
  }

  def conditionalZip( first: Source[Int, ActorRef],
                      second: Source[Int, ActorRef],
                      filler: Int,
                      condFcn: (Int, Int) => Boolean ): Source[(Int, Int), _] = {
    first.zipAll(second, filler, filler).statefulMapConcat{ () =>
      var prevList1 = List.empty[Int]
      var prevList2 = List.empty[Int]
      tuple => tuple match { case (e1, e2) =>
        if (e2 != filler) {
          if (e1 != filler && condFcn(e1, e2))
            (e1, e2) :: Nil
          else {
            if (e1 != filler)
              prevList1 :+= e1
            prevList2 :+= e2
            val (opElem1, rest1) = popFirstMatch(prevList1, condFcn(_, e2))
            opElem1 match {
              case None =>
                if (e1 != filler) {
                  val (opElem2, rest2) = popFirstMatch(prevList2, condFcn(e1, _))
                  opElem2 match {
                    case None =>
                      Nil
                    case Some(e) =>
                      prevList2 = rest2
                      (e1, e) :: Nil
                  }
                }
                else
                  Nil
              case Some(e) =>
                prevList1 = rest1
                (e, e2) :: Nil
            }
          }
        }
        else
          Nil
      }
    }
  }


  def test2 = {



    /*
    //// Case 1:
    val first = Source(1 :: 2 :: 4 :: 6 :: Nil)
    val second = Source(1 :: 2 :: 3 :: 4 :: 5 :: 6 :: 7 :: Nil)

    conditionalZip(first, second, Int.MinValue, _ == _).runForeach(println)
    // (1,1)
    // (2,2)
    // (4,4)
    // (6,6)

    conditionalZip(first, second, Int.MinValue, _ > _).runForeach(println)
    // (2,1)
    // (4,3)
    // (6,4)

    conditionalZip(first, second, Int.MinValue, _ < _).runForeach(println)
    // (1,2)
    // (2,3)
    // (4,5)
    // (6,7)



    //// Case 2:
    val first = Source(3 :: 9 :: 5 :: 5 :: 6 :: Nil)
    val second = Source(1 :: 3 :: 5 :: 2 :: 5 :: 6 :: Nil)

    conditionalZip(first, second, Int.MinValue, _ == _).runForeach(println)
    // (3,3)
    // (5,5)
    // (5,5)
    // (6,6)

    conditionalZip(first, second, Int.MinValue, _ > _).runForeach(println)
    // (3,1)
    // (9,3)
    // (5,2)
    // (6,5)

    conditionalZip(first, second, Int.MinValue, _ < _).runForeach(println)
    // (3,5)
    // (5,6)

     */
  }


  def test3 = {

    val completionMatcher: PartialFunction[Any, CompletionStrategy] = { case akka.actor.Status.Success(_) => CompletionStrategy.draining }
    val failureMatcher: PartialFunction[Any, Throwable] = {
      case Status.Failure(exception) =>
        exception
    }

    var ref1: ActorRef = null
    val source1: Source[Int, ActorRef] = Source.actorRef[Int](completionMatcher, failureMatcher, bufferSize = 100, OverflowStrategy.dropNew)
    source1.mapMaterializedValue(x => ref1 = x)


    var ref2: ActorRef = null

    val source2: Source[Int, ActorRef] = Source.actorRef[Int](completionMatcher, failureMatcher, bufferSize = 100, OverflowStrategy.dropNew)

    source2.mapMaterializedValue(x => ref2 = x)


    conditionalZip(source1, source2, Int.MinValue, _ == _).runForeach(println)


    ref1 ! 1
    ref2 ! 1

    ref1 ! 2
    ref2 ! 2

  }

  def test4 = {
    val fruitsAndDeniedCommands = Source(
      "banana" :: "pear" :: "orange" :: "deny:banana" :: "banana" :: "pear" :: "banana" :: Nil)

    val denyFilterFlow = Flow[String].statefulMapConcat { () =>
      var denyList = Set.empty[String]

      { element =>
        if (element.startsWith("deny:")) {
          denyList += element.drop("deny:".size)
          Nil // no element downstream when adding a deny listed keyword
        } else if (denyList(element)) {
          Nil // no element downstream if element is deny listed
        } else {
          element :: Nil
        }
      }
    }

    fruitsAndDeniedCommands.via(denyFilterFlow).runForeach(println)
  }


  def main(args: Array[String]): Unit = {

    test4


  }
}
