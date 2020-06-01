package IntelligentEmergencyDepartment.Config

import com.typesafe.config.ConfigFactory

import scala.jdk.CollectionConverters._

object Values{

  val URL_Waitingroom: List[String] = ConfigFactory.load().getStringList("urls-to-power-bi.URL_Waitingroom").asScala.toList
  val URL_InnerWaitingroom: List[String] = ConfigFactory.load().getStringList("urls-to-power-bi.URL_InnerWaitingroom").asScala.toList
  val URL_DoctorView: List[String]  = ConfigFactory.load().getStringList("urls-to-power-bi.URL_DoctorView").asScala.toList
  val URL_DepartmentOverview: List[String]  = ConfigFactory.load().getStringList("urls-to-power-bi.URL_DepartmentOverview").asScala.toList
  val URL_FilterLocation: List[String]  = ConfigFactory.load().getStringList("urls-to-power-bi.URL_FilterLocation").asScala.toList
  val URL_PandStatus:List[String]  = ConfigFactory.load().getStringList("urls-to-power-bi.URL_PandStatus").asScala.toList
  val URL_Corona_Rooms: List[String]  = ConfigFactory.load().getStringList("urls-to-power-bi.URL_Corona_Rooms").asScala.toList

  val PowerBiSubscription:String = ConfigFactory.load().getString("pub-sub.subscription")

  val hospitalFolderName: String = ConfigFactory.load().getString("hospitalFolderName")


}
