package tech.parasol.akka.workshop.data

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonInclude}
import com.fasterxml.jackson.annotation.JsonInclude.Include

import scala.beans.BeanProperty


trait DataEntity {
  def id: Option[String]
  def dataType: Option[Int]
  def operation: Int
  def data: Option[AnyRef]
}



trait ElasticDataEntity extends DataEntity {
  def index: String
  def typeName: Option[String]
  def version: Option[Int]
}

object OperationType {
  val operationDefault = 0
  val operationNew = 1
  val operationDelete = 2
  val operationUpdate = 3
  val operationUpdateArrayAppend = 4
  val operationUpdateArrayRemove = 5


  /**
    * HBase
    */
  val operationPut = 1
  /**
    * append means to append the value after existing value
    * e.g.
    * append value: 200
    * before: 100
    * after: 100200
    */

  val operationAppend = 6
  val operationIncrement = 7

}


object DataEntityOps {
  val dataTypeElasticSearch = 1
  val dataTypeHBase = 2
  val dataTypeKafka = 3
  val dataTypeMysql = 4
  val dataTypePostgres = 5
  val dataTypeRedis = 6

}
final case class ElasticJsonDataEntity(
                                        @JsonIgnore
                                        @JsonInclude(Include.NON_ABSENT)
                                        @BeanProperty
                                        id: Option[String] = None,
                                        @BeanProperty
                                        dataType: Option[Int] = Option(DataEntityOps.dataTypeElasticSearch),
                                        @BeanProperty
                                        index: String,
                                        @JsonIgnore
                                        @JsonInclude(Include.NON_ABSENT)
                                        @BeanProperty
                                        typeName: Option[String] = Option("_doc"),
                                        @BeanProperty
                                        operation: Int = OperationType.operationNew,
                                        @JsonIgnore
                                        @JsonInclude(Include.NON_ABSENT)
                                        @BeanProperty
                                        version: Option[Int] = None,
                                        @BeanProperty
                                        data: Option[AnyRef] = None
                                      ) extends ElasticDataEntity with Serializable


final case class TestData(id: String, data: String)