package IntelligentEmergencyDepartment.Domain.Transformation

import play.api.libs.json.{Json, OFormat}

/**
 * An overview of the patients. Contains the patients and the date when the overview was published
 * @param patients
 * @param date
 */
case class ElvisPatientsOverview(patients: List[ElvisPatient], date:String)

object ElvisPatientsOverview {
  implicit val personsFormat: OFormat[ElvisPatientsOverview] = Json.format[ElvisPatientsOverview]
}
