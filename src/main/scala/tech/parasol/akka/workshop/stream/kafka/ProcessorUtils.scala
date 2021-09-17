package tech.parasol.akka.workshop.stream.kafka

import java.io.FileNotFoundException
import java.nio.file.NoSuchFileException

import akka.stream.Supervision
import akka.util.Timeout
import org.slf4j.LoggerFactory
import scala.concurrent.duration._

object ProcessorUtils {

  implicit val logger = LoggerFactory.getLogger(this.getClass.getName)

  val retryTimes = 5


  implicit val timeout = Timeout(5 seconds)

  val decider: Supervision.Decider = {
    case e: FileNotFoundException => {
      logger.error(s"[FileNotFoundException] The document does not exist. Try to read the document after 20 seconds ===> ", e)
      Supervision.Resume
    }
    case e: NoSuchFileException => {
      logger.error(s"[NoSuchFileException] The document does not exist. Try to read the document after 20 seconds ===> ", e)
      Supervision.Resume
    }

    case e: java.lang.AssertionError => {
      logger.error(s"[AssertionError] The configuration is not applicable for app", e)
      Supervision.Resume
    }
    case e: Exception => {
      logger.error(s"[ActivityProcessCore] thrown during in processing ===> ", e)
      Supervision.Resume
    }
  }



}

