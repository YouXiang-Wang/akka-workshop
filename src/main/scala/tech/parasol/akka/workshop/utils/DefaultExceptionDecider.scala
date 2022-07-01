package tech.parasol.akka.workshop.utils

import java.io.FileNotFoundException

import akka.stream.Supervision
import org.slf4j.LoggerFactory

object DefaultExceptionDecider {

  implicit val logger = LoggerFactory.getLogger(this.getClass.getName)


  val decider: Supervision.Decider = {
    case e: FileNotFoundException => {
      logger.error(s"[FileNotFoundException] The document does not exist. Try to read the document after 20 seconds ===> ", e)
      Supervision.Resume
    }
    case e: ClassNotFoundException => {
      logger.error(s"[ClassNotFoundException] The required class does not exist ===> ", e)
      Supervision.Resume
    }
    case e: ClassCastException => {
      logger.error(s"[ClassCastException] Cast class exception ===> ", e)
      Supervision.Resume
    }
    case e: Exception => {
      logger.error(s"[DefaultExceptionDecider] thrown during processing ===> ", e)
      Supervision.Resume
    }
    case e: Throwable => {
      logger.error(s"[DefaultExceptionDecider] thrown during processing ===> ", e)
      Supervision.Resume
    }
  }


}
