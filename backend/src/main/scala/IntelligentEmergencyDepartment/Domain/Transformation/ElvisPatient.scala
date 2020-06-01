package IntelligentEmergencyDepartment.Domain.Transformation

import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.{Format, JsPath, Reads, Writes}

/**
 * Represents a patient in the elvis system
 */
object ElvisPatient {
  val timeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  implicit val eventReader: Reads[List[ElvisEvent]] = Reads.list(ElvisEvent.fEvent)

  val reads: Reads[ElvisPatient] = (
    (JsPath \ "CareContactId").read[Int] and
      (JsPath \ "CareContactRegistrationTime").read[String] and
      (JsPath \ "DepartmentComment" ).read[String] and
      (JsPath \ "Events").read[List[ElvisEvent]]and
      (JsPath \ "Location").read[String] and
      (JsPath \ "PatientId").read[Int] and
      (JsPath \ "ReasonForVisit").read[String] and
      (JsPath \ "Team").read[String] and
      (JsPath \ "VisitId").read[Int] and
      (JsPath \ "VisitRegistrationTime").read[String]
    )(ElvisPatient.apply _)

  val writes: Writes[ElvisPatient] = (
    (JsPath \ "CareContactId").write[Int] and
      (JsPath \ "CareContactRegistrationTime").write[String] and
      (JsPath \ "DepartmentComment" ).write[String] and
      (JsPath \ "Events").write[List[ElvisEvent]] and
      (JsPath \ "Location").write[String] and
      (JsPath \ "PatientId").write[Int] and
      (JsPath \ "ReasonForVisit").write[String] and
      (JsPath \ "Team").write[String] and
      (JsPath \ "VisitId").write[Int] and
      (JsPath \ "VisitRegistrationTime").write[String]
    )(unlift(ElvisPatient.unapply))

  implicit val fPatient: Format[ElvisPatient] = Format(reads, writes)
}

case class ElvisPatient(careContactId: Int,
                        careContactRegistrationTime: String,
                        departmentComment: String,
                        events: List[ElvisEvent],
                        location: String,
                        patientId: Int,
                        reasonForVisit: String,
                        team: String,
                        visitId: Int,
                        visitRegistrationTime: String)
