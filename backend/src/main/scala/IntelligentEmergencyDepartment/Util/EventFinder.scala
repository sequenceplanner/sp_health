package IntelligentEmergencyDepartment.Util

import IntelligentEmergencyDepartment.Domain.Transformation.{ElvisPatient, EmergencyPatient, Event}
import IntelligentEmergencyDepartment.Domain.Transformation.EventDataTypes.{Category, TypeOfEvent, Title}
import scala.language.postfixOps
import org.joda.time.DateTime

/**
 * Responsible for finding events that aren't included in the events that are given from elvis
 */
object EventFinder {


  def createCustomEventsForHospitalPatient(date:DateTime, currentPatient:ElvisPatient, newPatient:ElvisPatient): List[Event] = {
    val newEvent = createEventFromAttributeDifferenceHospital(date:DateTime, currentPatient, newPatient) _

    List(
      newEvent(_.departmentComment, Category.Custom, Title.DepartmentCommentUpdate, TypeOfEvent.DepartmentCommentUpdate),
      newEvent(_.reasonForVisit, Category.Custom, Title.ReasonForVisitUpdate, TypeOfEvent.ReasonForVisitUpdate),
      newEvent(_.team, Category.Custom, Title.TeamUpdate, TypeOfEvent.TeamUpdate),
      newEvent(p=>ChangeTimeZone.changeToSwedishTimeZoneReadable(p.visitRegistrationTime), Category.Custom, Title.VisitRegistrationTimeUpdate, TypeOfEvent.VisitRegistrationTimeUpdate)
    ) flatten
  }

  def createCustomEventsForEmergencyPatient(date:DateTime, currentPatient:EmergencyPatient, newPatient:EmergencyPatient): List[Event] = {
    val newEvent = createEventFromAttributeDifferenceEmergency(date:DateTime, currentPatient, newPatient) _

    List(
      newEvent(_.departmentComment, Category.Custom, Title.DepartmentCommentUpdate, TypeOfEvent.DepartmentCommentUpdate),
      newEvent(_.location, Category.Custom, Title.LocationUpdate, TypeOfEvent.LocationUpdate),
      newEvent(_.reasonForVisit.reason, Category.Custom, Title.ReasonForVisitUpdate, TypeOfEvent.ReasonForVisitUpdate),
      newEvent(_.clinic.name, Category.Custom, Title.TeamUpdate, TypeOfEvent.TeamUpdate),
      newEvent(p=>ChangeTimeZone.changeToSwedishTimeZoneReadable(p.visitRegistrationTime), Category.Custom, Title.VisitRegistrationTimeUpdate, TypeOfEvent.VisitRegistrationTimeUpdate)
    ) flatten
  }

  def createCustomEventsForNewEmergencyPatient(date:DateTime,newPatient: EmergencyPatient): List[Event] = {
    val newEvent = createEventFromAttributeEmergency(date,newPatient) _

    List(
      newEvent(_.departmentComment, Category.Custom, Title.DepartmentCommentUpdate, TypeOfEvent.DepartmentCommentUpdate),
      newEvent(_.location, Category.Custom, Title.LocationUpdate, TypeOfEvent.LocationUpdate),
      newEvent(_.reasonForVisit.reason, Category.Custom, Title.ReasonForVisitUpdate, TypeOfEvent.ReasonForVisitUpdate),
      newEvent(_.clinic.name, Category.Custom, Title.TeamUpdate, TypeOfEvent.TeamUpdate),
      newEvent(p=>ChangeTimeZone.changeToSwedishTimeZoneReadable(p.visitRegistrationTime), Category.Custom, Title.VisitRegistrationTimeUpdate, TypeOfEvent.VisitRegistrationTimeUpdate)
    ) flatten
  }

  def createCustomEventsForNewHospitalPatient(date:DateTime,newPatient: ElvisPatient): List[Event] = {
    val newEvent =createEventFromAttributeHospital(date,newPatient) _

    List(
      newEvent(_.departmentComment, Category.Custom, Title.DepartmentCommentUpdate, TypeOfEvent.DepartmentCommentUpdate),
      newEvent(_.reasonForVisit, Category.Custom, Title.ReasonForVisitUpdate, TypeOfEvent.ReasonForVisitUpdate),
      newEvent(_.team, Category.Custom, Title.TeamUpdate, TypeOfEvent.TeamUpdate),
      newEvent(p=>ChangeTimeZone.changeToSwedishTimeZoneReadable(p.visitRegistrationTime), Category.Custom, Title.VisitRegistrationTimeUpdate, TypeOfEvent.VisitRegistrationTimeUpdate)
    ) flatten


  }







  private def createEventFromAttributeEmergency(date:DateTime, newPatient: EmergencyPatient)(f: EmergencyPatient => String, category: Category, title:Title, eventType:TypeOfEvent) = {
    f(newPatient) match {
      case "None" => None
      case value => Some(Event(newPatient.careContactId,0, Some(category), title, eventType.name, value, newPatient.visitId,newPatient.patientId,date ))
    }
  }



  private def createEventFromAttributeHospital(date:DateTime, newPatient: ElvisPatient)(f: ElvisPatient => String, category: Category, title:Title, eventType:TypeOfEvent) = {
    f(newPatient) match {
      case "None" => None
      case value => Some(Event(newPatient.careContactId,0, Some(category), title, eventType.name, value, newPatient.visitId,newPatient.patientId,date ))
    }
  }



  private def createEventFromAttributeDifferenceEmergency(date:DateTime,currentPatient: EmergencyPatient, newPatient: EmergencyPatient)(f: EmergencyPatient => String, category: Category, title:Title, eventType:TypeOfEvent) = {
    (f(currentPatient), f(newPatient)) match {
      case (currValue, newValue) if currValue != newValue =>
        Some(Event(careContactId = newPatient.careContactId,careEventId = 0, Some(category), title, eventType.name, newValue, visitId = newPatient.visitId, newPatient.patientId, date ))

      case _ => None
    }
  }

  private def createEventFromAttributeDifferenceHospital(date:DateTime,currentPatient: ElvisPatient, newPatient: ElvisPatient)(f: ElvisPatient => String, category: Category, title:Title, eventType:TypeOfEvent) = {
    (f(currentPatient), f(newPatient)) match {
      case (currValue, newValue) if currValue != newValue =>
        Some(Event(newPatient.careContactId,0, Some(category), title, eventType.name, newValue, newPatient.visitId, newPatient.patientId ,date))

      case _ => None
    }
  }

}
