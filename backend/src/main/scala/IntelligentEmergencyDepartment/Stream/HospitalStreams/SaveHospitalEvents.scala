package IntelligentEmergencyDepartment.Stream.HospitalStreams

import java.time.LocalDateTime

import IntelligentEmergencyDepartment.Database.{HospitalEventRepository, HospitalPatientEvent}
import IntelligentEmergencyDepartment.Domain.Transformation.{ElvisPatient, ElvisPatientsOverview}
import IntelligentEmergencyDepartment.Domain.Transformation.EventDataTypes.{Category, TypeOfEvent, Title}
import IntelligentEmergencyDepartment.Stream.Sinks
import IntelligentEmergencyDepartment.Util.{Converter, EventFinder, TimeParser}
import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink}
import org.joda.time.DateTime

/**
 * Responsible for saving patients events from all the departments at the hospital.
 */
object SaveHospitalEvents {
  val hospitalEventTable = new HospitalEventRepository()

  val mainStream: Sink[ElvisPatientsOverview, NotUsed] = Flow[ElvisPatientsOverview].statefulMapConcat { () =>
    var oldPatients: List[ElvisPatient] = List()
    overview => {
      val newPatients = overview.patients.groupBy(_.patientId).filter{case (_,v)=>v.length <= 1}.flatMap{case(_,v)=>v}.toList
      val date = TimeParser.parseElvisPublishedTime(overview.date).getOrElse(DateTime.now())
      val toReturn: List[HospitalPatientEvent] = newPatients.map(newPat => {
        oldPatients.find(old => old.patientId == newPat.patientId) match {
          case Some(oldPat) =>
            EventFinder.createCustomEventsForHospitalPatient(date, newPat, oldPat)
          case None =>
            EventFinder.createCustomEventsForNewHospitalPatient(date, newPat)
        }
      }
      ).flatMap(_.map(a => {
        HospitalPatientEvent(a.careContactId, 0, a.category.getOrElse(Category("")).name
          ,
          TimeParser.toDatabaseDateFormat(date),
          a.title,
          a.eventType,
          a.value,
          a.visitId,
          a.patientId)
      }))

      val removedEvents: List[HospitalPatientEvent] = oldPatients.filterNot(oldPat =>
        newPatients.exists(_.patientId == oldPat.patientId))
        .map(oldPat =>
          HospitalPatientEvent(oldPat.careContactId, 0, Category.Custom.name,
            LocalDateTime.of(date.getYear, date.getMonthOfYear, date.getDayOfMonth, date.getHourOfDay, date.getMinuteOfHour, date.getSecondOfMinute),
            Title.RemovedPatient.text,
            TypeOfEvent.RemovedPatient.name,
            "",
            oldPat.visitId,
            oldPat.patientId))

      oldPatients = newPatients
      List(toReturn ++ removedEvents)
    }
  }.mapAsync(1)(p => hospitalEventTable.insert(p)).log("error hospital").to(Sinks.handleOptionalDbResponse("Hospital eventTable:"))

}
