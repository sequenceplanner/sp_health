package IntelligentEmergencyDepartment
import IntelligentEmergencyDepartment.Database.Db
import IntelligentEmergencyDepartment.Stream.Sinks
import IntelligentEmergencyDepartment.Config.Values
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Broadcast, GraphDSL, RunnableGraph, Unzip}
import IntelligentEmergencyDepartment.Domain.Transformation.{Department, ElvisPatientsOverview, EmergencyEventsHolder, EmergencyPatientsOverview, Team}
import IntelligentEmergencyDepartment.Domain.PowerBI.{JsonRoom, PandOverview, PowerBiRoom}
import IntelligentEmergencyDepartment.Stream.EmergencyStreams.{DoctorPatientsStream, EventsToDbStream, MapStream, UnitOverviewStream, WaitingRoomStream}
import IntelligentEmergencyDepartment.Stream.Flows.{GeneralFlows, LoggFlows}
import IntelligentEmergencyDepartment.Stream.HospitalStreams.{PandOverviewStream, SaveHospitalEvents}
import IntelligentEmergencyDepartment.Util.{JsonManager, Pubsub}


object Main extends App {
  println("Started")
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: ActorMaterializer = ActorMaterializer()

  system.registerOnTermination(() => Db.close())

  val subscription = Values.PowerBiSubscription
  println("Subscription: " + subscription)

  Db.initDatabase()
  initFromJson(Values.hospitalFolderName)

  val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    // broadcasts. Used to split the stream into tow or more.
    val broadcaster = b.add(Broadcast[ElvisPatientsOverview](3))
    val hospitalBroadCaster = b.add(Broadcast[ElvisPatientsOverview](2))
    val pandBroadCaster = b.add(Broadcast[List[PandOverview]](2))
    val emergencyBroadCaster = b.add(Broadcast[EmergencyPatientsOverview](6))
    val mapBroadCaster = b.add(Broadcast[List[PowerBiRoom]](2))


    val splitNewEventsAndOverview = b.add(Unzip[EmergencyPatientsOverview, List[EmergencyEventsHolder]])

    Pubsub.wrapInRestartSource(
      Pubsub.subscribeStream(subscription)
        .via(GeneralFlows.excludeOldMessages)
        .map(JsonManager.convertToPatientOverview)
        .collect({
          case Some(overview: ElvisPatientsOverview) => overview
        })) ~> LoggFlows.elvisLog("CareContactId", _.careContactId) ~> LoggFlows.elvisLog("PatientID", _.patientId) ~>  broadcaster.in

    //Print out that we have received an event
    broadcaster.out(0) ~> Sinks.printHospitalNewEvent

    //The hospital stream
    broadcaster.out(1) ~> hospitalBroadCaster.in
      hospitalBroadCaster ~>
        GeneralFlows.filterCorona ~> PandOverviewStream.convertToPandOverviews ~> pandBroadCaster.in
        pandBroadCaster ~> PandOverviewStream.sendOnline
        pandBroadCaster ~> PandOverviewStream.saveToDb
      hospitalBroadCaster ~> SaveHospitalEvents.mainStream

    //The emergency stream
    broadcaster.out(2) ~> GeneralFlows.convertToEmergencyPatientOverview ~>  LoggFlows.logUpdates ~> GeneralFlows.appendCustomEvents ~> GeneralFlows.diffNewEventsFlow ~> splitNewEventsAndOverview.in
      splitNewEventsAndOverview.out0 ~>
        emergencyBroadCaster ~> Sinks.printID
        emergencyBroadCaster ~> UnitOverviewStream.mainStream
        emergencyBroadCaster ~> DoctorPatientsStream.mainStream
        emergencyBroadCaster ~> WaitingRoomStream.waitingRoomStream
        emergencyBroadCaster ~> WaitingRoomStream.innerWaitingRoomStream
        emergencyBroadCaster ~> MapStream.convertToRooms ~>
          mapBroadCaster ~> MapStream.ordinaryMapStream
          mapBroadCaster ~> MapStream.coronaMapStream
      splitNewEventsAndOverview.out1 ~> EventsToDbStream.mainStream


    ClosedShape
  })
  graph.run()


  /**
   * Fetches the needed information from the given folder.
   * @param hospitalFolder The folder that contains config-files about the rooms and the teams
   */
  def initFromJson(hospitalFolder: String): Unit = {
    val jsonDeps = JsonManager.readJson("src/main/resources/" + hospitalFolder + "/units.json")

    Department.initPossibleDepartments(JsonManager.initListAs[Department](jsonDeps, "departments"))
    Team.initPossibleTeams(JsonManager.initListAs[Team](jsonDeps, "teams"))

    JsonRoom.initPossibleRooms(
      JsonManager.initListAs[JsonRoom](
        JsonManager.readJson("src/main/resources/" + hospitalFolder + "/rooms.json"), "rooms"))
  }
}
