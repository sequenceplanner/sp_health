package IntelligentEmergencyDepartment.Domain.Transformation

import play.api.libs.json.{Format, Json}

/**
 * An overview of the patients. Contains the patients and the date when the overview was published
 * @param patients the patients at the emergency department
 * @param date the date this overview was published
 */
case class EmergencyPatientsOverview(patients: List[EmergencyPatient], date:String)

object EmergencyPatientsOverview {
  implicit val personsFormat: Format[EmergencyPatientsOverview] = Json.format[EmergencyPatientsOverview]
}
