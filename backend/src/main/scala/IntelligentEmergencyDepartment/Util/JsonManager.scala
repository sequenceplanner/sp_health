package IntelligentEmergencyDepartment.Util

import java.io.FileInputStream

import akka.stream.alpakka.googlecloud.pubsub.PubSubMessage
import IntelligentEmergencyDepartment.Domain.Transformation.ElvisPatientsOverview
import IntelligentEmergencyDepartment.Config.LocaleTimeConfig
import play.api.libs.json.{JsError, JsObject, JsSuccess, JsValue, Json, Reads}
import org.joda.time._

/**
 * Responsible for converting json to scala-objects
 */
object JsonManager {


  def convertToJson(s:String): JsValue ={
    Json.parse(s)
  }

  /**
   * Attempts to convert a PubSubMessage to ElvisPatientsOverview
   * @param message the PubSubMEssage to convert
   * @return An optional ElvisPatientOverview
   */
  def convertToPatientOverview(message: PubSubMessage):Option[ElvisPatientsOverview] = {
    message.data match {
      case Some(data) =>
        getPatientsOverviewFromJsObject(addPublishedDate(convertToJson(data).as[JsObject],message.publishTime.toString))
      //last argument of addPublishedDate just converts the time from UTC to UTC + 01:00 (Stockholm)
      case None => None
    }
  }

  private[this] def getPatientsOverviewFromJsObject(jsObject: JsObject): Option[ElvisPatientsOverview] = {
    val result = Json.fromJson[ElvisPatientsOverview](jsObject)
    result match{
      case JsSuccess(value, _) => Some(value)
      case JsError(_) => None
    }
  }

  private[this] def addPublishedDate(jsObject: JsObject, publishedDate:String): JsObject ={
    jsObject + ("date" -> Json.toJson(publishedDate))
  }


  def readJson(name:String):JsValue={
    val stream = new FileInputStream(name)
    val json = try {
      Json.parse(stream)
    } finally {
      stream.close()
    }
    json
  }

  def initListAs[T](value:JsValue, jsonIdentifier:String)(implicit read:Reads[List[T]]): List[T] = {
    (value \ jsonIdentifier ).as[List[T]]
  }

  /**
   * Inits the part of the json value corresponding to the given identifier as an object of the given type
   * @param value the json
   * @param jsonIdentifier the identifier
   * @param read implicit jsonreader
   * @tparam T The type to convert to
   * @return
   */
  def initValueAs[T](value:JsValue, jsonIdentifier:String)(implicit read:Reads[T]): T ={
    (value \ jsonIdentifier).as[T]
  }

}
