package tech.parasol.akka.workshop.utils


import java.net.Socket

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.collection.JavaConverters._
import scala.util.control.Breaks.{break, breakable}

object CommonUtils {

  def createActor[T](system: ActorSystem, clazz: Class[T], name: String = ""): ActorRef = {
    if(name.length == 0) system.actorOf(Props(clazz), name) else system.actorOf(Props(clazz))

  }

  @inline
  def isAllDigits(x: String) = x forall Character.isDigit

  def getFirstNoDigitStr(array: Array[(String, String)]) = {
    var index = 0
    val length = array.length
    breakable(
      for( a <- 0 until length) {
        val s = array(a)
        if(!isAllDigits(s._2)) {
          break
        }
        index = index + 1
      }
    )
    array(index)
  }


  def getFirstConfigStr(array: Array[String]) = {
    var index = 0
    val length = array.length
    breakable(
      for( a <- 0 until length) {
        val s = array(a)
        if(s != null && s.length > 0) {
          break
        }
        index = index + 1
      }
    )
    array(index)
  }

  def int2BinArray(intValue: Int): Array[Int] = {
    int2BinSeq(intValue).toArray
  }

  def int2BinSeq(intValue: Int): Seq[Int] = {
    val zero = Seq(0)
    val init = Seq(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31)
    intValue match {
      case 0 => zero
      case _ => {
        val s = init.filterNot( x => (intValue & (0x01 << x)) == 0 ).map( x => (0x01 << x) )
        s
      }
    }
  }

  def paramsGetString(params: Map[String, String], key: String, default: String = ""): String = {
    var ret = default
    if (params.contains(key)) {
      ret = params(key)
    }
    ret
  }


  def long2BinArray(longValue: Long): Array[Int] = {
    long2BinSeq(longValue).toArray
  }

  def long2BinSeq(longValue: Long): Seq[Int] = {
    val zero = Seq(0)
    val init = Seq(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31)
    longValue match {
      case 0 => zero
      case _ => {
        val s = init.filterNot( x => (longValue & (0x01 << x)) == 0 ).map(x => (0x01 << x) )
        s
      }
    }
  }


  def javaListToSet[T](list: java.util.List[T]) = {
    try {
      if(list == null) Set.empty[T] else list.asScala.toSet[T]
    } catch {
      case exception: Exception => {
        exception.printStackTrace()
        Set.empty[T]
      }
    }
  }

  @inline
  def fillString(src: String, params: String*) = {
    src.format(params :_*)
  }

  @inline
  def fixIndexName(indexName: String) = {
    indexName.toLowerCase
  }


  def availablePort(ports: Seq[Int]) = {
    ports.toStream.filterNot(port => {
      ServiceUtils.isPortListening("localhost", port)
    }).take(1).head
  }
}

object ServiceUtils {
  def isPortListening(hostName: String, portNumber: Int) = try {
    new Socket(hostName, portNumber).close()
    true
  } catch {
    case _: Exception =>
      false
  }
}

