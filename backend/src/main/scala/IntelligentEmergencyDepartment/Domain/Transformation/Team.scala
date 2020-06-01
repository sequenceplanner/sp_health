package IntelligentEmergencyDepartment.Domain.Transformation

import play.api.libs.json.{Format, JsPath, Reads, Writes}
import play.api.libs.functional.syntax.{unlift, _}

/**
 * Represents a team at the emergency department
 */
case class Team(name:String, commonName:String, reasonsToVisit:List[ReasonToVisit], locationHead:Option[String], department:Option[String]){
  def canEqual(a: Any): Boolean = a.isInstanceOf[Team]


  override def equals(that: Any): Boolean =
    that match {
      case that: Team =>
        that.canEqual(this) &&
          this.name == that.name &&
          this.commonName == that.commonName
      case _ => false
    }
}

object Team {
  var possibleTeams:List[Team] = List()

  def initPossibleTeams(departments:List[Team]): Unit ={
    possibleTeams=departments
  }

  def getTeamByName(name:String): Option[Team] = {
    possibleTeams.find(p=>p.name==name)
  }

  def getTeamByReasonToVisit(reasonToVisit:String):Option[Team] = {
    possibleTeams.find(p=>p.reasonsToVisit.exists(r => r.reason==reasonToVisit))
  }

  def getTeamByLocationAbbrev(locationAbbrev:String): Option[Team] = {
    possibleTeams.find(p=> p.locationHead match{
      case Some(head) => head == locationAbbrev
      case None => false
    })
  }

  def getTeamByDepartment(departmentName:String): Option[Team] = {
    possibleTeams.find(p=> p.department match{
      case Some(head) => head == departmentName
      case None => false
    })
  }

  implicit val readTeam: Reads[Team] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "commonName").read[String] and
      (JsPath \ "reasonsToVisit").read[List[ReasonToVisit]] and
      (JsPath \ "locationHead").readNullable[String] and
      (JsPath \ "department").readNullable[String]
    )(Team.apply _)

  implicit val writeTeam: Writes[Team] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "commonName").write[String] and
      (JsPath \ "reasonsToVisit" ).write[List[ReasonToVisit]] and
      (JsPath \ "locationHead").writeOptionWithNull[String] and
      (JsPath \ "department").writeOptionWithNull[String]
    )(unlift(Team.unapply))



  implicit val teamFormat: Format[Team] = Format(readTeam, writeTeam)
  implicit val TeamsRead: Reads[List[Team]] = Reads.list(teamFormat)


}
