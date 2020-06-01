package IntelligentEmergencyDepartment.Stream.EmergencyStreams

import IntelligentEmergencyDepartment.Config.Values
import IntelligentEmergencyDepartment.Domain.PowerBI.{InnerWaitingroom, Waitingroom}
import IntelligentEmergencyDepartment.Domain.Transformation.EmergencyPatientsOverview
import IntelligentEmergencyDepartment.Stream.Flows.GeneralFlows
import IntelligentEmergencyDepartment.Stream.{Diff, DiffHttpResponses, Sinks}
import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}

/**
 * Fetches the information needed for the "PatientVy"
 */
object WaitingRoomStream {

  def waitingRoomStream()(implicit mat:ActorMaterializer): Sink[EmergencyPatientsOverview, NotUsed] =
    Flow[EmergencyPatientsOverview].map(Waitingroom.apply)
      .via(GeneralFlows.diffFlow("WaitingRoom", (patientView:Waitingroom)=> Diff.waitingroomDiff(patientView) || !DiffHttpResponses.getWaitingroomLastPostStatus))
      .via(GeneralFlows.sendSerializableItem(Values.URL_Waitingroom))
      .to(Sinks.handleHttpResponse(DiffHttpResponses.setWaitingroomLastPostStatus))

  def innerWaitingRoomStream()(implicit mat:ActorMaterializer): Sink[EmergencyPatientsOverview, NotUsed] =
    Flow[EmergencyPatientsOverview].map(InnerWaitingroom.createInnerWaitingroomList)
      .via(GeneralFlows.diffFlow("InnerWaitingroom", (waiting:List[InnerWaitingroom])=> Diff.innerWaitingroomDiff(waiting) || !DiffHttpResponses.getInnerWaitingroomLastPostStatus))
      .via(GeneralFlows.sendSerializable(Values.URL_InnerWaitingroom))
      .to(Sinks.handleHttpResponse(DiffHttpResponses.setInnerWaitingroomLastPostStatus))



}
