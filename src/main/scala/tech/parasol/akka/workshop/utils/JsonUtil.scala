package tech.parasol.akka.workshop.utils


import java.lang.reflect.{ParameterizedType, Type}

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import com.fasterxml.jackson.core.`type`.TypeReference

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object JsonUtil {
  implicit val mapper = new ObjectMapper() with ScalaObjectMapper {
    registerModule(DefaultScalaModule)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
      .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
  }

  implicit val withNullMapper = new ObjectMapper() with ScalaObjectMapper {
    registerModule(DefaultScalaModule)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
      .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
      .setSerializationInclusion(JsonInclude.Include.ALWAYS)
  }


  def toJson(value: Any, withNull: Boolean = false)(implicit mapper: ObjectMapper = mapper): String = {
    if(withNull)
      withNullMapper.writeValueAsString(value)
    else
      mapper.writeValueAsString(value)
  }

  def toMap[V](json:String)(implicit m: Manifest[V]) = fromJson[Map[String,V]](json)

  def fromJson[T](json: String)(implicit m : Manifest[T]): T = {
    mapper.readValue[T](json)
  }

  def convert[T: Manifest](str: String): T = {
    mapper.readValue(str, typeReference[T])
  }

  def convert2Nested[T: ClassTag](str: String): T = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    mapper.readValue(str, clazz).asInstanceOf[T]
  }

  def getFieldInt(json: String, key: String, errorValue: Int): Int = {
    val node = mapper.readValue[ObjectNode](json)
    try {
      if(node.has(key)) {
        node.get(key).asInt()
      } else errorValue
    } catch {
      case e :Exception => {
        e.printStackTrace()
        errorValue
      }
    }
  }

  def getFieldString(json: String, key: String) = {
    val node = mapper.readValue[ObjectNode](json)
    if(node.has(key)) node.get(key).asText("") else ""

  }

  def convertList[T: ClassTag](str: String) = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    val listType = mapper.getTypeFactory.constructCollectionType(classOf[java.util.List[T]], clazz)
    val s: java.util.List[T] = mapper.readValue(str, listType)
    s.asScala.toList
  }


  def getInstance[T](json: String)(implicit ct: ClassTag[T]): T =
    Try {
      mapper.readValue(json, ct.runtimeClass).asInstanceOf[T]
    } match {
      case Success(instance) => instance
      case Failure(e) => throw new IllegalStateException(s"Error during parsing of '$json'", e)
    }


  private def typeFromManifest(m: Manifest[_]): Type = {
    if (m.typeArguments.isEmpty) { m.erasure }
    else new ParameterizedType {
      def getRawType = m.erasure
      def getActualTypeArguments = m.typeArguments.map(typeFromManifest).toArray
      def getOwnerType = null
    }
  }

  private def typeReference[T: Manifest] = new TypeReference[T] {
    override def getType = typeFromManifest(manifest[T])
  }

  def parseJsonAs[T: Manifest](str: String): T = {
    mapper.readValue(str, typeReference[T])
  }

}
