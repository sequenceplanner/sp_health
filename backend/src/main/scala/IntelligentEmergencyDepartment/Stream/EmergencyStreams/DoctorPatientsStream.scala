package IntelligentEmergencyDepartment.Stream.EmergencyStreams

import IntelligentEmergencyDepartment.Config.Values
import IntelligentEmergencyDepartment.Domain.PowerBI.DoctorPatient
import IntelligentEmergencyDepartment.Domain.Transformation.EmergencyPatientsOverview
import IntelligentEmergencyDepartment.Stream.Flows.GeneralFlows
import IntelligentEmergencyDepartment.Stream.{Diff, DiffHttpResponses, Sinks}
import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}

object DoctorPatientsStream {

  def mainStream()(implicit mat:ActorMaterializer): Sink[EmergencyPatientsOverview, NotUsed] =
    Flow[EmergencyPatientsOverview].map(ppo=>{ppo.patients.map(patient=> DoctorPatient(patient,ppo))})
      .via(GeneralFlows.diffFlow("Doctor Patient", (docPatients:List[DoctorPatient])=> Diff.patientDoctorDiff(docPatients) || !DiffHttpResponses.getDocPatientsLastPostStatus))
      .via(GeneralFlows.sendSerializable(Values.URL_DoctorView))
        .to(Sinks.handleHttpResponse(DiffHttpResponses.setDocPatientsLastPostStatus))

}
