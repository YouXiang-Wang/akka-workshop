package tech.parasol.akka.workshop.cluster

import scala.concurrent.Future

object Utils {

  implicit val ec = Application.system.dispatcher

  def loadSharedInfo: Future[Seq[SharedInfo]] = Future {
    Thread.sleep(3000)
    val s1 = SharedInfo("profile_1", "share_1")
    val s2 = SharedInfo("profile_2", "share_2")
    val s3 = SharedInfo("profile_3", "share_3")

    Seq(s1, s2, s3)

  }
}
