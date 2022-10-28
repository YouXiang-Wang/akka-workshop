package tech.parasol.akka.workshop.model

import scala.collection.mutable.ListBuffer

class Person(age:Int,name:String,salary:Int) {
  var AGE = age
  var NAME = name
  var SALARY = salary


  override def toString = s"Person($AGE, $NAME, $SALARY)"



}


object TestCompare {

  def sortRule(person: Person): (Int, Int) = {
    (person.AGE, person.SALARY)
  }

  def testSortBy: Unit = {
    val personOne = new Person(18, "zhangsan", 10000)
    val personTwoA = new Person(20, "lisiA", 9000)
    val personTwoB = new Person(20, "lisiB", 10000)
    val personThree = new Person(15, "wangwu", 8000)
    var list = new ListBuffer[Person]
    list.+=(personOne, personTwoA,personTwoB, personThree)

    println(list.sortBy(sortRule)(Ordering.Tuple2(Ordering.Int.reverse, Ordering.Int.reverse)))
  }


  def main(args: Array[String]): Unit = {


    testSortBy

  }
}


