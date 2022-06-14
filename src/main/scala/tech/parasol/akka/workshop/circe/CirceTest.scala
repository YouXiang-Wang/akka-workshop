package tech.parasol.akka.workshop.circe

import io.circe.Decoder
import tech.parasol.akka.workshop.cluster.User
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.syntax._

import scala.reflect.ClassTag
import io.circe._, io.circe.parser._


object CirceTest {

  //implicit val fooDecoder: Decoder[User] = deriveDecoder[User]


  def main(args: Array[String]): Unit = {


    val user1 = User("u1", "n1")

    val str1 = user1.asJson
    println("str1 ===> " + str1)


    val str2 = Seq(user1).asJson
    println("str2 ===> " + str2)



    val seq = str2.as[Seq[User]] match {
      case Right(value) => value
    }

    println("seq ===> " + seq)


  }
}
