package IntelligentEmergencyDepartment.Stream.EmergencyStreams

import IntelligentEmergencyDepartment.Config.Values
import IntelligentEmergencyDepartment.Domain.PowerBI.{JsonRoom, PowerBiRoom}
import IntelligentEmergencyDepartment.Domain.Transformation.EmergencyPatientsOverview
import IntelligentEmergencyDepartment.Stream.Flows.GeneralFlows
import IntelligentEmergencyDepartment.Stream.{Diff, DiffHttpResponses, Sinks}
import IntelligentEmergencyDepartment.Util.FilterLocation
import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}

/**
 * Fetches the information needed for the "Kartvy" and "CoronaKarta"
 */
object MapStream {

  val convertToRooms:Flow[EmergencyPatientsOverview, List[PowerBiRoom], NotUsed] =
    Flow[EmergencyPatientsOverview].map(ppo=>{
      FilterLocation.filter(ppo,JsonRoom.possible_rooms)
    })

  def ordinaryMapStream()(implicit mat:ActorMaterializer): Sink[List[PowerBiRoom], NotUsed] =
    GeneralFlows.diffFlow("Rooms", (rooms:List[PowerBiRoom])=> Diff.roomsDiff(rooms) || !DiffHttpResponses.getRoomsLastPostStatus)
      .via(GeneralFlows.sendSerializable(Values.URL_FilterLocation))
      .to(Sinks.handleHttpResponse(DiffHttpResponses.setRoomsLastPostStatus))

  def coronaMapStream()(implicit mat:ActorMaterializer): Sink[List[PowerBiRoom], NotUsed] =
    Flow[List[PowerBiRoom]].map(rooms=>
      rooms.map(room=>{
        val patientsWithCoronaInRoom = room.Patients.count(p => p.reasonForVisit.reason.toLowerCase == "pand")
        if(patientsWithCoronaInRoom > 0){
          room.copy( NumOccupants = patientsWithCoronaInRoom, Color = "#B12323")
        }else{
          room.copy(NumOccupants = patientsWithCoronaInRoom,Color = "#858585")
        }
      })
    )
    .via(GeneralFlows.diffFlow("Corona Rooms",
      (rooms:List[PowerBiRoom])=> Diff.roomsCoronaDiff(rooms) || !DiffHttpResponses.getCoronaRoomsLastPostStatus))
    .via(GeneralFlows.sendSerializable(Values.URL_Corona_Rooms))
      .to(Sinks.handleHttpResponse(DiffHttpResponses.setCoronaRoomsLastPostStatus))

}
