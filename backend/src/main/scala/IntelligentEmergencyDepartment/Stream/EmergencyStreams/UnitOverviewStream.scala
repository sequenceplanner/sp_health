package IntelligentEmergencyDepartment.Stream.EmergencyStreams

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import IntelligentEmergencyDepartment.Config.Values
import IntelligentEmergencyDepartment.Database.{EmergencyPatientEvent, EventType, PatientEvent}
import IntelligentEmergencyDepartment.Domain.PowerBI.UnitOverview
import IntelligentEmergencyDepartment.Domain.Transformation.EventDataTypes.TypeOfEvent
import IntelligentEmergencyDepartment.Domain.Transformation.{Department, EmergencyPatientsOverview, FrequencyStatus}
import IntelligentEmergencyDepartment.Stream.Flows.GeneralFlows
import IntelligentEmergencyDepartment.Stream.{Diff, DiffHttpResponses, Sinks, Sources}
import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}

import scala.util.{Failure, Success, Try}

/**
 * Fetches the information that is needed for the "PatientVy"
 */
object UnitOverviewStream {
  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  def mainStream(implicit mat:ActorMaterializer): Sink[EmergencyPatientsOverview, NotUsed] =
    fetchPatientEvents().log("error")
      .via(convertToUnitOverviews).log("error")
      .via(GeneralFlows.diffFlow("UnitOverview",
        (overViews:List[UnitOverview])=> Diff.departmentsDiff(overViews) || !DiffHttpResponses.getUnitOverviewLastPostStatus))
      .via(GeneralFlows.sendSerializable(Values.URL_DepartmentOverview)).to(Sinks.handleHttpResponse(DiffHttpResponses.setUnitOverviewLastPostStatus))



  private def fetchPatientEvents()(implicit mat: ActorMaterializer): Flow[EmergencyPatientsOverview, (EmergencyPatientsOverview, Try[Seq[EmergencyPatientEvent]]), NotUsed] =
    Flow[EmergencyPatientsOverview].mapAsync(1)(p=>Sources.getEventsWithRecover(p,60))

  /**
   * Converts a tuple of a PowerbiPatientsOverview and a Seq with patient events to a list of UnitOverviews
   */
  private val convertToUnitOverviews: Flow[(EmergencyPatientsOverview, Try[Seq[EmergencyPatientEvent]]), List[UnitOverview], NotUsed] = Flow[(EmergencyPatientsOverview,Try[Seq[EmergencyPatientEvent]])].map {
    case (overview, Success(seq)) =>
      //Group on carecontactID
      println("Events fetched: " + seq.length)
      val now = LocalDateTime.now()
      val patients = seq.groupBy(_.careContactId)
        .map{case(k,v)=> (k,
        v.filter(_.Type==TypeOfEvent.TeamUpdate.name).maxByOption(_.Time) match {
          case Some(e)=>e.Value
          case None => println(v)
                      "NONE"

        },v.partition(_.Time.isBefore(LocalDateTime.now().minusMinutes(60))))}




      val byDepartment =
        patients.groupBy(_._2).map{case(k,v)=>(Department.getDepartment(k),v)}.collect{case(Some(k),v)=>(k,v)}

      val frequencies = byDepartment.map(patients=>{
        var finishedChange = 0
        var attendedChange = 0
        var unAttendedChange = 0


        patients._2.foreach{p=>

        val removedNow = hasType(p._3._2,TypeOfEvent.RemovedPatient)
        val finishedNow = hasType(p._3._2,TypeOfEvent.Finished)
        val hasBeenFinished = finishedNow || hasType(p._3._1,TypeOfEvent.Finished)
        val attendedNow = hasType(p._3._2, TypeOfEvent.Attended)
        val hasBeenAttended = attendedNow || hasType(p._3._1, TypeOfEvent.Attended)
        val registrationTime = (p._3._1 ++ p._3._2).filter(_.Type==TypeOfEvent.VisitRegistrationTimeUpdate.name)
          .map(e=>(e.Time,LocalDateTime.parse(e.Value, formatter))).maxBy(_._1)._2
        val newNow = equalOrAfter(registrationTime,now.minusMinutes(60))

        if(removedNow){
          if(hasBeenFinished){
            finishedChange -=1
          }else if(hasBeenAttended){
            attendedChange -=1
          }else {
            unAttendedChange-=1
          }
          }
        else if(finishedNow) {
          finishedChange += 1
          if (hasBeenAttended) {
            attendedChange -= 1
          } else{
            unAttendedChange -= 1
          }
        }
        else if(attendedNow) {
          if(!hasBeenFinished){
            attendedChange += 1
            unAttendedChange -= 1
          }
        }
        else if(newNow){
              unAttendedChange += 1
          }
      }
        FrequencyStatus(patients._1, unAttendedChange, attendedChange, finishedChange)
      })
      UnitOverview.createUnitOverviews(overview, frequencies.toList)
    case (o, Failure(e)) =>
      println("Failed to read from db: " + e.getMessage)
      UnitOverview.createUnitOverviews(o, List())
  }

  def hasType(list:Seq[EmergencyPatientEvent], customType: TypeOfEvent): Boolean ={
    list.exists(_.Type==customType.name)
  }


  def equalOrAfter(one: LocalDateTime, two:LocalDateTime): Boolean = {
    one.compareTo(two) >= 0
  }

}
