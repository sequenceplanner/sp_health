package IntelligentEmergencyDepartment.Domain.PowerBI

import IntelligentEmergencyDepartment.Domain.JsonSerializable
import IntelligentEmergencyDepartment.Domain.Transformation.EmergencyPatientsOverview
import IntelligentEmergencyDepartment.Util.ChangeTimeZone
import play.api.libs.json.{Format, JsValue, Json}

// PRIMARY CONSTRUCTOR
case class Waitingroom(
                          date : String,
                          nrOfWaiting: Int,
                          nrOfTriaged: Int,
                          hospital: String,
                          averageTTT: Long,
                          medicin : Int,
                          kirurgi : Int,
                          ortopedi : Int,
                          jour : Int,
                          annat : Int,
                        ) extends JsonSerializable{
  def asJson(): JsValue = Json.toJson(this) //When sending it to Power BI later, converting to Json is needed
}
object Waitingroom{
  implicit val patientViewFormat: Format[Waitingroom] = Json.format[Waitingroom]

  // ALTERNATE CONSTRUCTOR
  def apply(
             powerBiPatientsOverview:EmergencyPatientsOverview //Need the overview to get date
           ): Waitingroom = {

    var nrOfWaiting = 0
    var nrOfTriaged = 0
    var medicin = 0
    var kirurgi = 0
    var ortopedi = 0
    var jour = 0
    var annat = 0
    val average = Average.calculateNewTTTAverage(powerBiPatientsOverview)

    for(patient <- powerBiPatientsOverview.patients){
      if(patient.location == "yvr" || patient.location == "" || patient.location == " "){ //matching with posible location names of the outer waitingroom
        nrOfWaiting += 1
      }
      else{
        nrOfTriaged += 1
        patient.clinic.commonName match {
            case "ME1" | "ME2" | "ME3" | "ME4" => medicin += 1;
            case "KI" => kirurgi += 1;
            case "OR" => ortopedi += 1;
            case "LILA" => jour += 1;
            case _ => annat += 1;
        }
      }
    }
    Waitingroom(ChangeTimeZone.changeToSwedishTime(powerBiPatientsOverview.date),nrOfWaiting,nrOfTriaged, "NAL", average, medicin,kirurgi,ortopedi, jour, annat) //This will be returned
  }
}
