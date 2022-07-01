package tech.parasol.akka.workshop.model

import tech.parasol.akka.workshop.cluster.RawData

import scala.beans.BeanProperty


final case class UserProfile(
                       userId: String,
                       userName: Option[String] = None
                     )

final case class Category(id: String, cateName: String, order: Int = 1)


final case class UserPurchase(user: UserProfile, categories: Seq[Category])


final case class Item(id: String, itemName: String)




