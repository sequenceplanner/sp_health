package IntelligentEmergencyDepartment.Stream.EmergencyStreams

import IntelligentEmergencyDepartment.Database.{EmergencyEventRepository, EmergencyPatientEvent}
import IntelligentEmergencyDepartment.Domain.Transformation.EmergencyEventsHolder
import IntelligentEmergencyDepartment.Domain.Transformation.EventDataTypes.Category
import IntelligentEmergencyDepartment.Stream.Sinks
import IntelligentEmergencyDepartment.Util.TimeParser
import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


/**
 * Responsible for storing events that belongs to patients at the emergency department
 */
object EventsToDbStream {
  val eventTable = new EmergencyEventRepository()
  val mainStream: Sink[List[EmergencyEventsHolder], NotUsed] =
    Flow[List[EmergencyEventsHolder]].mapAsync(1)(list=>
      Future.sequence(list.flatMap(createEvents).map(t=>eventTable.insert(t))))
      .to(Sinks.handleDbResponses("Update emergency event table"))


  private def createEvents(emergencyEventsHolder: EmergencyEventsHolder) = {
    emergencyEventsHolder.newEvents.map(p => {
      val date = p.time
      EmergencyPatientEvent(p.careContactId,
        p.careEventId,
        p.category.getOrElse(Category("")).name,
        TimeParser.toDatabaseDateFormat(date),
        p.title,
        p.eventType,
        p.value,
        p.visitId,
        p.patientId)})
  }
}
