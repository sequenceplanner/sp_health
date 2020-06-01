package IntelligentEmergencyDepartment.Domain.Transformation


import IntelligentEmergencyDepartment.Util.TimeParser
import IntelligentEmergencyDepartment.Domain.Transformation.EventDataTypes.{Category, Title}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import play.api.libs.json._


object Event {
  val dateFormatter: DateTimeFormatter = TimeParser.elvisEventDateFormatter
  implicit val dateWriter: Writes[DateTime] = (dateTime: DateTime) => {
    JsString(dateTime.toString(dateFormatter))
  }

  implicit val dateReader: Reads[DateTime] = (json: JsValue) => JsSuccess(DateTime.parse(json.toString, dateFormatter))

  implicit val fDateTime: Format[DateTime] = Format(dateReader, dateWriter)
  implicit val fPowerBiEvent: Format[Event] = Json.format[Event]
}

case class Event(
                          careContactId:Int,
                          careEventId: Int,
                          category: Option[Category] = None,
                          title: Title = Title(""),
                          eventType: String = "",
                          value: String = "",
                          visitId: Int,
                          patientId: Int,
                          time: DateTime = TimeParser.now()
                     ) {


}
