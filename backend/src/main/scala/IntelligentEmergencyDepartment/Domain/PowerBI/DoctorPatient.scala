package IntelligentEmergencyDepartment.Domain.PowerBI

import IntelligentEmergencyDepartment.Domain.JsonSerializable
import play.api.libs.json.{Format, JsValue, Json}

import IntelligentEmergencyDepartment.Util.ChangeTimeZone

import IntelligentEmergencyDepartment.Domain.Transformation.{EmergencyPatient, EmergencyPatientsOverview}


// PRIMARY CONSTRUCTOR
case class DoctorPatient( doctor: String,
                          patient: Int,
                          date : String,
                        )extends JsonSerializable{
  def asJson(): JsValue = Json.toJson(this) //When sending it to Power BI later, converting to Json is needed
}
object DoctorPatient{
  implicit val doctorPatientFormat: Format[DoctorPatient] = Json.format[DoctorPatient]

  // ALTERNATE CONSTRUCTOR
  def apply(
             Patient: EmergencyPatient,
             powerBiPatientsOverview:EmergencyPatientsOverview //Need the overview to get date
           ): DoctorPatient = {

    val doctor: String = Patient.doctorId
    val patient: Int = Patient.careContactId
    DoctorPatient(doctor, patient, ChangeTimeZone.changeToSwedishTime(powerBiPatientsOverview.date)) //This will be returned
  }
}
