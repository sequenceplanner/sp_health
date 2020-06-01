package IntelligentEmergencyDepartment.Domain.PowerBI

import IntelligentEmergencyDepartment.Domain.JsonSerializable
import IntelligentEmergencyDepartment.Domain.Transformation.EmergencyPatientsOverview
import IntelligentEmergencyDepartment.Util.ChangeTimeZone
import play.api.libs.json.{Format, JsValue, Json}

// PRIMARY CONSTRUCTOR
case class InnerWaitingroom(
                          date : String,
                          department : String,
                          treated : Int,
                          waiting : Int,
                          averageTTL : Long,
                        ) extends JsonSerializable{
  implicit val patientViewFormat: Format[InnerWaitingroom] = Json.format[InnerWaitingroom]
  def asJson(): JsValue = Json.toJson(this) //When sending it to Power BI later, converting to Json is needed
}
//Below function is used to create multible InnerWaitingroom objects from an EmergencyPatientsOverview object
//The number of inner waitingrooms and the name of the departments is hard coded
object InnerWaitingroom{

 def createInnerWaitingroomList(
             ppo:EmergencyPatientsOverview //Need the overview to get date
           ): List[InnerWaitingroom] = {

             var me2 = 0
             var me2wait = 0
             var me3 = 0
             var me3wait = 0
             var or = 0
             var orwait = 0
             var ki = 0
             var kiwait = 0
             var me4jour = 0
             var me4jourwait = 0
             val date = ChangeTimeZone.changeToSwedishTime(ppo.date)

             for(patient <- ppo.patients){
               if(patient.location == "ivr" || patient.location == "iv"){
                 patient.clinic.commonName match {
                   case "ME2" => me2wait += 1
                   case "ME3" => me3wait += 1
                   case "OR"  => orwait += 1
                   case "KI"  => kiwait += 1
                   case "ME4" | "LILA" => me4jourwait += 1
                   case _ => //do nothing
                 }
               }
               else if(patient.location != "yvr" && patient.location != "" && patient.location != " "){
                 patient.clinic.commonName match {
                   case "ME2" => me2 += 1
                   case "ME3" => me3 += 1
                   case "OR"  => or += 1
                   case "KI"  => ki += 1
                   case "ME4" | "LILA" => me4jour += 1
                   case _ => //do nothing
                 }
               }
             }
             val averages = Average.calculateNewTTLAverage(ppo)
             val list : List[InnerWaitingroom] = List(InnerWaitingroom(date,"Medicin2",me2,me2wait,averages(0)),
                                                 InnerWaitingroom(date,"Medicin3",me3,me3wait,averages(1)),
                                                 InnerWaitingroom(date,"Ortopedi",or,orwait,averages(2)),
                                                 InnerWaitingroom(date,"Kirurgi",ki,kiwait,averages(3)),
                                                 InnerWaitingroom(date,"Jour/Medicin4",me4jour,me4jourwait,averages(4)))

             list //returning the List
  }
}
