package IntelligentEmergencyDepartment.Domain.PowerBI

import IntelligentEmergencyDepartment.Domain.JsonSerializable
import play.api.libs.json.{Format, JsValue, Json}

/**
 * The representation of the pandemic-situation at a department used by power bi.
 * @param date The date when this information was accurate
 * @param Department The department
 * @param NumberOFPand Number of patients
 */
case class PandOverview(date:String, Department:String, NumberOFPand:Int) extends JsonSerializable {
  def asJson(): JsValue = Json.toJson(this)
}

object PandOverview{
  implicit val pandOverviewtFormat: Format[PandOverview] = Json.format[PandOverview]
}
