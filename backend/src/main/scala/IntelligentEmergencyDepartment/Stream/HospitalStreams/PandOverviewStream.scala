package IntelligentEmergencyDepartment.Stream.HospitalStreams

import IntelligentEmergencyDepartment.Config.Values
import IntelligentEmergencyDepartment.Database.{PandStatus, PandStatusRepository}
import IntelligentEmergencyDepartment.Domain.PowerBI.PandOverview
import IntelligentEmergencyDepartment.Domain.Transformation.ElvisPatient
import IntelligentEmergencyDepartment.Stream.Flows.GeneralFlows
import IntelligentEmergencyDepartment.Stream.{Diff, DiffHttpResponses, Sinks}
import IntelligentEmergencyDepartment.Util.{ChangeTimeZone, TimeParser}
import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import org.joda.time.DateTime

/**
 * Contains streams that fetches information about the pandemic from the patients,
 * sends info about the pandemic to power bi and saves info to db.
 *
 */
object PandOverviewStream {
  val pandTable = new PandStatusRepository()


  val convertToPandOverviews: Flow[(DateTime, List[ElvisPatient]), List[PandOverview], NotUsed] =
    Flow[(DateTime, List[ElvisPatient])].map(epo=>{
    val date = ChangeTimeZone.changeToSwedishTimeZone(epo._1).toLocalDateTime.toString()
    epo._2.groupBy(_.team)
      .map { case (k, v) => PandOverview(date, k, v.length) }.toList
    }).via(GeneralFlows.diffFlow("PandStatus", (pandOverviews:List[PandOverview]) => Diff.pandStatusDiff(pandOverviews) || !DiffHttpResponses.getPandOverviewLastPostStatus))

  def  sendOnline()(implicit mat:ActorMaterializer): Sink[List[PandOverview], NotUsed] =
    Flow[List[PandOverview]].via(GeneralFlows.sendSerializable(Values.URL_PandStatus))
    .to(Sinks.handleHttpResponse(DiffHttpResponses.setPandOverviewLastPostStatus))

  val saveToDb: Sink[List[PandOverview], NotUsed] = Flow[List[PandOverview]].map(_.map(p=>{
    PandStatus(
      TimeParser.toDatabaseDateFormat(
        ChangeTimeZone.changeToSwedishTimeZone(
          TimeParser.parseElvisPublishedTime(p.date).getOrElse(DateTime.now()))),
      p.Department,
      p.NumberOFPand
      )})).mapAsync(1)(p => pandTable.insert(p)).log("error pand db").to(Sinks.handleOptionalDbResponse("Pandemi Clinics Table:"))



}
