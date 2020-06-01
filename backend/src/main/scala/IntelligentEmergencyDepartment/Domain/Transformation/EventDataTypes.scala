package IntelligentEmergencyDepartment.Domain.Transformation

import play.api.libs.json._
import scala.concurrent.duration._
import scala.language.postfixOps

object EventDataTypes {
  case class Category(name: String) {
    def removed = Category(name + "-removed")
  }

  object Category {

    val Custom = Category("Custom")
    val T = Category("T")
    val U = Category("U")
    val Q = Category("Q")
    val Priority = Category("P")
    val B = Category("B")

    implicit val jsFormat: Format[Category] = Json.format[Category]
  }

  case class TypeOfEvent(name:String)
  object TypeOfEvent {
    val VisitRegistrationTimeUpdate: TypeOfEvent = TypeOfEvent("VisitRegistrationTimeUpdate")

    val RemovedPatient = TypeOfEvent("RemovedPatient")
    val NewPatient = TypeOfEvent("NewPatient")
    val DepartmentCommentUpdate = TypeOfEvent("DepartmentCommentUpdate")
    val LocationUpdate = TypeOfEvent("LocationUpdate")
    val ReasonForVisitUpdate = TypeOfEvent("ReasonForVisitUpdate")
    val TeamUpdate = TypeOfEvent("TeamUpdate")

    val Attended = TypeOfEvent("LÄKARE")
    val Finished = TypeOfEvent("KLAR")
  }

  case class VisitReason(reason: String)
  object VisitReason {
    val ClinicalTrial = VisitReason("AKP")
    val All = VisitReason("ALL")
    val Trauma1 = VisitReason("TRAUMAN1")
    val Trauma2 = VisitReason("TRAUMAN2")
    val Trauma3 = VisitReason("TRAUMAN3")
    val B = VisitReason("B")
    val Mep = VisitReason("MEP")
    val Höft = VisitReason("HÖFT")
    val Pand = VisitReason("PAND")


    implicit val jsFormat: Format[VisitReason] = Json.format[VisitReason]
  }

  case class Title(text: String)
  object Title {
    val VisitRegistrationTimeUpdate: Title = Title("Uppdatering av starttid")

    val Empty = Title("")
    val Finished = Title("Klar")
    val Plan = Title("Plan")
    val ScanOrClinic = Title("Rö/klin")
    val Doctor = Title("Läkare")

    val NewPatient = Title("Ny Patient")
    val RemovedPatient = Title("Borttagen Patient")
    val DepartmentCommentUpdate = Title("Avdelningskommentar")
    val LocationUpdate = Title("Rumsuppdatering")
    val ReasonForVisitUpdate = Title("Besöksanledning uppdatering")
    val TeamUpdate = Title("Avdelning uppdatering")



    implicit def asString(title: Title): String = title.text

    implicit val jsFormat: Format[Title] = Json.format[Title]
  }


  /**
   * Triage is a degree of seriousness on the condition of a patient.
   * The different degrees are represented by numbers as [1, 2, 3, 4 5]
   * or by colors as [Red, Orange, Yellow, Green, Blue].
   */
  final case class TriageLevelValue(id: String, urgency: Int) extends Ordered[TriageLevelValue] {
    override def compare(that: TriageLevelValue): Int = this.urgency compare that.urgency
  }
  object TriageLevel {
    type TriageColor = TriageLevelValue

    val Blue = TriageLevelValue("blue", 5)
    val Green = TriageLevelValue("green", 4)
    val Yellow = TriageLevelValue("yellow", 3)
    val Orange = TriageLevelValue("orange", 2)
    val Red = TriageLevelValue("red", 1)
    val NotTriaged = TriageLevelValue("NotTriaged", 1)

    def fromString(id: String): TriageColor = id.toLowerCase match {
      case "blue" => Blue
      case "green" => Green
      case "yellow" => Yellow
      case "orange" => Orange
      case "red" => Red
      case _ => NotTriaged
    }

    /**
     * After this time has passed, a patient should receive attention
     */
    def checkupTime(color: TriageColor): Long = color match {
      case Blue | Green | Yellow | NotTriaged => 60.minutes.toMillis
      case Orange => 20.minutes.toMillis
      case Red => 0l
      case _ => Long.MaxValue
    }

    implicit val reads: Reads[TriageColor] = (__ \ "priority").read[String].map(fromString)
    implicit val writes: Writes[TriageColor] = (__ \ "priority").write[TriageColor]((b: TriageColor) => JsString(b.id))
    implicit val jsFormat: Format[TriageColor] = Json.format[TriageColor]
    def fromElvisValue(value: String): TriageColor = {
      val mapping = Map(
        "blå" -> TriageLevel.Blue.id,
        "grön" -> TriageLevel.Green.id,
        "gul" -> TriageLevel.Yellow.id,
        "orange" -> TriageLevel.Orange.id,
        "röd" -> TriageLevel.Red.id
      ) withDefaultValue "N/A"

      fromString(mapping(value toLowerCase))
    }
  }

}
