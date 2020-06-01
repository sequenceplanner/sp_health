package IntelligentEmergencyDepartment.Util

import IntelligentEmergencyDepartment.Config.LocaleTimeConfig
import IntelligentEmergencyDepartment.Domain.Transformation.ElvisPatient
import org.joda.time._

object ChangeTimeZone{
  def changeToSwedishTimeZoneReadable(time:String): String ={
    DateTime.parse(time).withZone(DateTimeZone.forID(LocaleTimeConfig.timeLocation)).toLocalDateTime.toString("yyyy-MM-dd HH:mm:ss")
  }


  def changeToSwedishTime(time:String) : String = {
    DateTime.parse(time).withZone(DateTimeZone.forID(LocaleTimeConfig.timeLocation)).toLocalDateTime.toString()

  }

  def changeToSwedishTimeZone(dateTime:DateTime): DateTime = {
    dateTime.withZone(DateTimeZone.forID(LocaleTimeConfig.timeLocation))
  }


}
