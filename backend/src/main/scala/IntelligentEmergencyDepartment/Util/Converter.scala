package IntelligentEmergencyDepartment.Util

import IntelligentEmergencyDepartment.Domain.Transformation.EventDataTypes.TriageLevel.TriageColor
import IntelligentEmergencyDepartment.Domain.Transformation.EventDataTypes.{Category, TypeOfEvent, Title, TriageLevel, VisitReason}
import IntelligentEmergencyDepartment.Domain.Transformation.{Department, ElvisEvent, ElvisPatient, ElvisPatientsOverview, EmergencyPatient, EmergencyPatientsOverview, Event, Team}
import org.joda.time.DateTime

import scala.language.postfixOps

/**
 * Responsible for converting elvis patients to emergency patients and elvis events to emergency events
 */
object Converter {



  def convertToEmergencyOverview(elvisOverview: ElvisPatientsOverview): EmergencyPatientsOverview = {
    EmergencyPatientsOverview(elvisOverview.patients.flatMap(elvisToEmergencyPatient),
      elvisOverview.date)
  }

  /**
   * Creates an EricaEvent from an ElvisEvent.
   */
  private def elvisToEmergencyEvent(careContactID:Int, patientId:Int,  e: ElvisEvent): Event = {
    Event(careContactID, e.CareEventId, Some(Category(e.Category)), Title(e.Title), e.Type, e.Value, e.VisitId, patientId, parseElvisTime(e))

  }

  private def elvisToEmergencyPatient(p:ElvisPatient):Option[EmergencyPatient] = {

    val clinic = Department.getDepartment(p.team)

    //If the patients team doesn't match any clinic, they don't belong to the emergency department
    if(clinic.isEmpty){
      return None
    }

    val powerBiEvents:List[Event] = p.events.map(e=>elvisToEmergencyEvent(p.careContactId,p.patientId,e))

    val latestValue = powerBiEvents.groupBy(_.category).flatMap { case (mbCategory, categoryEvents) =>
      mbCategory map { _ -> getLatestEvent(categoryEvents).fold("")(_.value) }
    }.withDefaultValue("None")

    val relevantCategories = Seq(Category.T, Category.U, Category.Q)
    val relevantEvents = powerBiEvents filter { _.category exists { relevantCategories contains } }

    val latestRelevantEvent = getLatestEvent(relevantEvents)
    val latestEventName: Title = latestRelevantEvent.fold(Title.Empty)(_.title)
    val latestEventTimeDiff: Long = latestRelevantEvent.fold(0L)(e => TimeParser.timePassedSince(e.time))

    val latestNursingEvent = getLatestEvent(powerBiEvents filter {_.category contains Category.B})
    val latestNursingEventTimeDiff: Long = latestNursingEvent.fold(0l)(e => TimeParser.timePassedSince(e.time))

    val doctorId = latestDoctorId(powerBiEvents)

    val triageColor = latestTriageColor(eventsWith(powerBiEvents, Category.Priority))

    Some(EmergencyPatient(
      p.careContactId,
      p.departmentComment,
      p.location,
      VisitReason(p.reasonForVisit),
      clinic.get,
      getTeam(clinic, p.location, VisitReason(p.reasonForVisit)),
      triageColor,
      powerBiEvents,
      latestEventName,
      latestEventTimeDiff,
      latestNursingEventTimeDiff,
      doctorId nonEmpty,
      doctorId getOrElse "N/A",
      latestEventTimeDiff > TriageLevel.checkupTime(triageColor),
      getLatestEvent(powerBiEvents) exists { _.title == Title.ScanOrClinic },
      eventsWith(powerBiEvents,Category.T) exists { _.title == Title.Plan },
      eventsWith(powerBiEvents,Category.T) exists { _.title == Title.Finished },
      p.visitId,
      p.patientId,
      p.visitRegistrationTime,
      latestValue(Category.Q)))

  }



  def getTeam(clinic: Option[Department], location: String, reasonForVisit: VisitReason): Option[Team] = {
    def byLocation:Option[Team] = location.headOption match{
      case Some(head) => Team.getTeamByLocationAbbrev(head.toString)
      case _ => None
    }

    // If location does not match, use clinic to determine team
    def byClinic:Option[Team] = clinic match {
      case Some(c) => Team.getTeamByDepartment(c.name)
      case _ => None
    }

    def byReasonToVisit:Option[Team] = {
      Team.getTeamByReasonToVisit(reasonForVisit.reason)
    }

    byReasonToVisit.orElse(byLocation).orElse(byClinic)
  }

  def eventsWith(events: List[Event], category: Category): List[Event] = events filter { _.category contains category }

  def latestDoctorId(events: List[Event]): Option[String] = {
    val doctorEvents = (e: Event) => e.category.contains(Category.T) && e.title == Title.Doctor
    getLatestEvent(events filter doctorEvents) map { _.value }
  }

  def latestTriageColor(events: List[Event]): TriageColor = {
    def toTriageLevel = (e: Event) => TriageLevel.fromElvisValue(e.title)

    val priorityEvents = events
      .filter { _.category.contains(Category.Priority) }

    getLatestEvent(priorityEvents) map toTriageLevel getOrElse TriageLevel.NotTriaged
  }

  /**
  Returns the latest event among list of events
   */
  private def getLatestEvent(events: List[Event]): Option[Event] = {
    if (events nonEmpty) Some(events maxBy { _.time }) else None
  }

  /**
   * @return The start time of the event, or the end time if start time is not parsable. If neither is parsable,
   *         the current time is returned.
   */
  private def parseElvisTime(event: ElvisEvent): DateTime = {
    (TimeParser.parseElvisEventTime(event.Start), TimeParser.parseElvisEventTime(event.End)) match {
      case (Some(t0), _) => t0
      case (None, Some(t1)) => t1
      case _ => TimeParser.now()
    }
  }









}
