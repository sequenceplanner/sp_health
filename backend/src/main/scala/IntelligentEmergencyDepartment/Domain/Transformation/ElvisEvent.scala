package IntelligentEmergencyDepartment.Domain.Transformation

import play.api.libs.json.{Format, Json}

/**
 * Represents an event in the elvis system
 */
object ElvisEvent {
  val timeFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
  implicit val fEvent: Format[ElvisEvent] = Json.format[ElvisEvent]
  val possibleCategories: List[String] = List("T", "Q", "U", "B", "P")


}
case class ElvisEvent(CareEventId: Int,
                      Category: String,
                      End: String,
                      Start: String,
                      Title: String,
                      Type: String,
                      Value: String,
                      VisitId: Int)
