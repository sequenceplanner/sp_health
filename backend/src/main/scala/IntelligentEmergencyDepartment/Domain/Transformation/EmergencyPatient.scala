package IntelligentEmergencyDepartment.Domain.Transformation

import IntelligentEmergencyDepartment.Domain.Transformation.EventDataTypes.TriageLevel.TriageColor
import IntelligentEmergencyDepartment.Domain.Transformation.EventDataTypes.VisitReason
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

/**
 * Represents a patient
 */
object EmergencyPatient {
  implicit val fTriageLevel: Format[TriageColor] = EventDataTypes.TriageLevel.jsFormat
  implicit val fEricaPatient: Format[EmergencyPatient] = Json.format[EmergencyPatient]

}

case class EmergencyPatient(careContactId: Int,
                            departmentComment: String,
                            location: String,
                            reasonForVisit: VisitReason,
                            clinic: Department,
                            team:Option[Team],
                            priority: TriageColor,
                            events:List[Event],
                            latestEventName: String,
                            latestEventTimeDiff: Long,
                            latestNursingEventTimeDiff: Long,
                            isAttended: Boolean,
                            doctorId: String,
                            needsAttention: Boolean,
                            onExamination: Boolean,
                            hasPlan: Boolean,
                            isFinished: Boolean,
                            visitId: Int,
                            patientId:Int,
                            visitRegistrationTime: String,
                            entryMethod: String){
  def getTimeOfFirstEvent: Option[DateTime] = {
    if (events.nonEmpty) Some(events.minBy{ _.time }.time) else None
  }
}
