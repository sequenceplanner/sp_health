package IntelligentEmergencyDepartment.Stream.Flows

import IntelligentEmergencyDepartment.Domain.Transformation.{ElvisPatient, ElvisPatientsOverview, EmergencyPatientsOverview}
import akka.NotUsed
import akka.stream.scaladsl.Flow

object LoggFlows {
  /**
   * Logs if a patient has switched location or switched team
   *
   */

  val logUpdates: Flow[EmergencyPatientsOverview, EmergencyPatientsOverview, NotUsed] = Flow[EmergencyPatientsOverview].statefulMapConcat { () =>
    var currentPatients: EmergencyPatientsOverview = EmergencyPatientsOverview(List(), "")
    newPatients => {
      newPatients.patients.foreach(newPat => {
        currentPatients.patients.find(ep => ep.careContactId == newPat.careContactId) match {
          case Some(prev) =>
            if (newPat.location != prev.location) {
              println(s"Patient ${newPat.careContactId} moved from location ${prev.location} to ${newPat.location}")
            }
            if (newPat.priority != prev.priority) {
              println(s"Patient ${newPat.careContactId} changed from priority ${prev.priority.id} to ${newPat.priority.id}")
            }
          case None => //Do Nothing
        }
      })
    }
      currentPatients = newPatients
      List(newPatients)
  }

  def elvisLog(tag:String, f:ElvisPatient => Int): Flow[ElvisPatientsOverview, ElvisPatientsOverview, NotUsed] = Flow[ElvisPatientsOverview].statefulMapConcat { () =>
    var currentPatients: List[ElvisPatient] = List()
    newPatients => {
      newPatients.patients.foreach(newPat => {
        currentPatients.find(ep => f(ep)==f(newPat)) match {
          case Some(prev) =>
            if (newPat.team != prev.team) {
              println(s"$tag: Patient ${f(newPat)} moved from location ${prev.team} to ${newPat.team}")
            }
          case None => println(s"$tag: New Patient $newPat")
        }
      })

      currentPatients.filterNot(oldPat=>newPatients.patients.exists(newPat => f(newPat) ==f(oldPat))).foreach(p=>println(s"$tag: Patient Removed: $p"))
    }
      currentPatients = newPatients.patients
      List(newPatients)
  }
  val powerBiLog: Flow[EmergencyPatientsOverview, EmergencyPatientsOverview, NotUsed] = Flow[EmergencyPatientsOverview].map { overview =>
    println("Number of patients " + overview.patients.length)
    overview.patients.foreach(p => {
      println("ID: " + p.visitId + " Clinic: " + p.clinic.commonName + " Location: " + p.location + " ReasonToVisit: " + p.reasonForVisit)
    })
    overview
  }

}
