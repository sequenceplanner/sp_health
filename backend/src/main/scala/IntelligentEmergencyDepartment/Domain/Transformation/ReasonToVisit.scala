package IntelligentEmergencyDepartment.Domain.Transformation

import play.api.libs.json.{Format, Json, Reads}

case class ReasonToVisit(reason:String)

object ReasonToVisit{
  implicit val reasonToVisitFormat: Format[ReasonToVisit] = Json.format[ReasonToVisit]
  implicit val reasonsToVisitReads: Reads[List[ReasonToVisit]] = Reads.list(reasonToVisitFormat)
}
