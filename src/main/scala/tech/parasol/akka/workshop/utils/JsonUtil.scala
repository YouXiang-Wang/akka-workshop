package tech.parasol.akka.workshop.utils



import java.lang.reflect.{ParameterizedType, Type}

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._
import scala.reflect.{ClassTag, _}
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

  def addStringField(srcJson: String, key: String, value: String): String = {
    val objectNode = mapper.readTree(srcJson).asInstanceOf[ObjectNode]
    objectNode.put(key, value)
    objectNode.toString()
  }

  def addStringFieldMap(srcJson: String, map: Map[String, String]): String = {
    val objectNode = mapper.readTree(srcJson).asInstanceOf[ObjectNode]
    map.map(r => objectNode.put(r._1, r._2))
    objectNode.toString()
  }

  private def typeFromManifest(m: Manifest[_]): Type = {
    if (m.typeArguments.isEmpty) { m.runtimeClass }
    else new ParameterizedType {
      def getRawType = m.runtimeClass
      def getActualTypeArguments = m.typeArguments.map(typeFromManifest).toArray
      def getOwnerType = null
    }
  }

  private def typeReference[T: Manifest] = new TypeReference[T] {
    override def getType = typeFromManifest(manifest[T])
  }

  def fromMap[T: TypeTag: ClassTag](m: Map[String, _]) = {
    val rm = runtimeMirror(classTag[T].runtimeClass.getClassLoader)
    val classTest = typeOf[T].typeSymbol.asClass
    val classMirror = rm.reflectClass(classTest)
    val constructor = typeOf[T].decl(termNames.CONSTRUCTOR).asMethod
    val constructorMirror: MethodMirror = classMirror.reflectConstructor(constructor)

    val constructorArgs = constructor.paramLists.flatten.map((param: Symbol) => {
      val paramName = param.name.toString
      if(param.typeSignature <:< typeOf[Option[Any]])
        m.get(paramName)
      else
        m.get(paramName).getOrElse(throw new IllegalArgumentException("Map is missing required parameter named " + paramName))
    })

    constructorMirror(constructorArgs:_*).asInstanceOf[T]
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

  def convertList[T: ClassTag](str: String): List[T] = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    val listType = mapper.getTypeFactory.constructCollectionType(classOf[java.util.List[T]], clazz)
    val s: java.util.List[T] = mapper.readValue(str, listType)
    s.asScala.toList
  }


  def convert2Seq[T: ClassTag](str: String): Seq[T] = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    val listType = mapper.getTypeFactory.constructCollectionType(classOf[java.util.List[T]], clazz)
    val s: java.util.List[T] = mapper.readValue(str, listType)
    s.asScala
  }



  def getInstance[T](json: String)(implicit ct: ClassTag[T]): T =
    Try {
      mapper.readValue(json, ct.runtimeClass).asInstanceOf[T]
    } match {
      case Success(instance) => instance
      case Failure(e) => throw new IllegalStateException(s"Error during parsing of '$json'", e)
    }


  def convertByClassTag[T: ClassTag](str: String): T = {
    mapper.readValue(str, typeReferenceFromClassTag[T])
  }

  private def typeFromClassTag(m: ClassTag[_]): Type = {

    if (m.typeArguments.isEmpty) { m.runtimeClass }
    else new ParameterizedType {
      def getRawType = m.runtimeClass
      def getActualTypeArguments = m.typeArguments.map(x => tagToType2).toArray
      def getOwnerType = null
    }
  }

  private def typeReferenceFromClassTag[T: ClassTag] = new TypeReference[T] {
    override def getType = typeFromClassTag(classTag[T])
  }

  import scala.reflect.runtime.{universe => ru}
  import ru.TypeTag


  private def tagToType[T](implicit tag: scala.reflect.runtime.universe.TypeTag[T]): java.lang.reflect.Type = tag.mirror.runtimeClass(tag.tpe)
  private def tagToType2[T](implicit tag: scala.reflect.ClassTag[T]): java.lang.reflect.Type = scala.reflect.classTag[T].runtimeClass

  def convertByTypeTag[T: TypeTag](str: String): T = {
    mapper.readValue(str, typeReferenceFromTypeTag[T])
  }

  private def typeFromTypeTag(m: TypeTag[_]): java.lang.reflect.Type = {

    if (m.tpe.typeArgs.isEmpty) {
      m.mirror.runtimeClass(m.tpe)
    } else {
      new ParameterizedType {
        def getRawType = m.mirror.runtimeClass(m.tpe)
        def getActualTypeArguments = m.tpe.typeArgs.map(x =>
          tagToType
        ).toArray
        def getOwnerType = null
      }
    }
  }

  private def typeReferenceFromTypeTag[T: TypeTag] = new TypeReference[T] {
    override def getType = typeFromTypeTag(typeTag[T])
  }



  def parseJsonAs[T: Manifest](str: String): T = {
    mapper.readValue(str, typeReference[T])
  }


}
