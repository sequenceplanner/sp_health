package IntelligentEmergencyDepartment.Domain.PowerBI

import IntelligentEmergencyDepartment.Domain.JsonSerializable
import IntelligentEmergencyDepartment.Domain.Transformation.EventDataTypes.TriageLevel
import IntelligentEmergencyDepartment.Domain.Transformation.{Department, EmergencyPatient, EmergencyPatientsOverview, FrequencyStatus, Team}
import IntelligentEmergencyDepartment.Util.ChangeTimeZone
import play.api.libs.json.{JsValue, Json, Writes}

/**
 * The representation of a unit (team or department) used in power bi
 *
 * @param date The date when this presentation is correct
 * @param name The name of the unit
 */

class UnitOverview(val date: String,
                   val name: String,
                   val unattended_change: Int,
                   val attended_change: Int,
                   val finished_change: Int,
                   val isDepartment: Boolean) extends JsonSerializable {
  var nr_of_patients = 0
  var attended = 0
  var unattended = 0
  var finished = 0
  var red_patients = 0
  var orange_patients = 0
  var yellow_patients = 0
  var green_patients = 0
  var blue_patients = 0
  var grey_patients = 0

  private def addPatient(Patient: EmergencyPatient): Unit = {
    nr_of_patients += 1
    if (Patient.isFinished) {
      finished += 1
    }
    else if (Patient.isAttended) {
      attended += 1
    }
    else {
      unattended += 1
    }


    Patient.priority match {
      case TriageLevel.Blue => blue_patients += 1
      case TriageLevel.Green => green_patients += 1
      case TriageLevel.Orange => orange_patients += 1
      case TriageLevel.Red => red_patients += 1
      case TriageLevel.Yellow => yellow_patients += 1
      case TriageLevel.NotTriaged => grey_patients += 1
      case _ => grey_patients += 1
    }
  }

  def asJson(): JsValue = Json.toJson(this)

  /**
   * Checks whether the given department overview has the same statistics.
   *
   * @param that the department overview to compare with
   * @return Whether the given department has the same statistics
   */
  def sameStatistics(that: UnitOverview): Boolean = {
    this.name == that.name &&
      this.unattended_change == that.unattended_change &&
      this.attended_change == that.attended_change &&
      this.finished_change == that.finished_change &&
      this.nr_of_patients == that.nr_of_patients &&
      this.attended == that.attended &&
      this.unattended == that.unattended &&
      this.finished == that.finished &&
      this.red_patients == that.red_patients &&
      this.orange_patients == that.orange_patients &&
      this.yellow_patients == that.yellow_patients &&
      this.green_patients == that.green_patients &&
      this.grey_patients == that.grey_patients


  }


}


object UnitOverview {


  def createUnitOverviews(powerBiPatientsOverview: EmergencyPatientsOverview, changesPerDepartment: List[FrequencyStatus]): List[UnitOverview] = {
    var result: Map[String, UnitOverview] = Map()

    val date = ChangeTimeZone.changeToSwedishTime(powerBiPatientsOverview.date)

    Department.possibleDepartments.foreach(c => {
      val departmentsWithCommonName = changesPerDepartment.filter(fs => fs.dep.commonName == c.commonName)
      result += (c.commonName -> new UnitOverview(date, c.commonName,
        departmentsWithCommonName.map(_.unAttendedChange).sum,
        departmentsWithCommonName.map(_.attendedChange).sum,
        departmentsWithCommonName.map(_.finishedChange).sum, true))
    })


    Team.possibleTeams.foreach(t => result += (t.name -> new UnitOverview(date, t.commonName, 0, 0, 0, false)))

    powerBiPatientsOverview.patients.foreach(patient => {
      val team = patient.team
      if (team.isDefined && Team.possibleTeams.contains(team.get)) {
        result.get(team.get.name) match {
          case Some(departmentOverview) => departmentOverview.addPatient(patient)
          case None =>
            val newDepartment = new UnitOverview(date, team.get.commonName, 0, 0, 0, false)
            newDepartment.addPatient(patient)
            result += (team.get.name -> newDepartment)

        }
      }


      result.get(patient.clinic.commonName) match {
        case Some(departmentOverview) => departmentOverview.addPatient(patient)
        case None =>
          val newDepartment = new UnitOverview(date, patient.clinic.commonName, 0, 0, 0, true)
          newDepartment.addPatient(patient)
          result += (patient.clinic.commonName -> newDepartment)

      }

    })

    result.values.toList
  }

  implicit val writes: Writes[UnitOverview] = (departmentOverview: UnitOverview) => {
    Json.obj(
      "date" -> departmentOverview.date,
      "department" -> departmentOverview.name,
      "nr_of_patients" -> departmentOverview.nr_of_patients,
      "attended" -> departmentOverview.attended,
      "unattended" -> departmentOverview.unattended,
      "finished" -> departmentOverview.finished,
      "attended_change" -> departmentOverview.attended_change,
      "unattended_change" -> departmentOverview.unattended_change,
      "finished_change" -> departmentOverview.finished_change,
      "red_patients" -> departmentOverview.red_patients,
      "orange_patients" -> departmentOverview.orange_patients,
      "yellow_patients" -> departmentOverview.yellow_patients,
      "green_patients" -> departmentOverview.green_patients,
      "blue_patients" -> departmentOverview.blue_patients,
      "grey_patients" -> departmentOverview.grey_patients,
      "is_department" -> departmentOverview.isDepartment

    )
  }


}
