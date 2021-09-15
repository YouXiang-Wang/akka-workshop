package tech.parasol.akka.workshop.cluster

import scala.beans.BeanProperty

final case class User(
                     @BeanProperty
                       userId: String,
                     @BeanProperty
                       userName: String
                     )



final case class SharedInfo(
                             @BeanProperty
                             profileId: String,
                             @BeanProperty
                             shareId: String) {
  override def toString: String = s"[profileId = ${profileId}, shareId = ${shareId}]"
}


final case class ProfileInfo(profileId: String,
                             userList: Seq[User],
                             sharedList: Seq[SharedInfo])


object ProfileAction {

  final case class AddShare(share: SharedInfo)

  final case class RemoveShare(shareId: String)

  final case class GetShare(profileId: String)

  final case class GetProfile(profileId: String)

}



