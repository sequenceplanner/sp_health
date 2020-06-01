package IntelligentEmergencyDepartment.Domain.PowerBI

import IntelligentEmergencyDepartment.Domain.JsonSerializable
import IntelligentEmergencyDepartment.Domain.Transformation.EmergencyPatient
import play.api.libs.json.{Format, JsValue, Json, Writes}

object PowerBiRoom {


  implicit val writes: Writes[PowerBiRoom] = (room:PowerBiRoom) => {
    Json.obj(
      "Name" -> room.Name,
      "PosX" -> room.PosX,
      "PosY" -> room.PosY,
      "Width" -> room.Width,
      "Height" -> room.Height,
      "Category" -> room.Category,
      "WaitingRoom" -> room.WaitingRoom,
      "BedOutsideRoom" -> room.BedOutsideRoom,
      "NumOccupants" -> room.NumOccupants,
      "Date" -> room.Date,
      "Color" -> room.Color

    )
  }
}


case class PowerBiRoom(
                        Name: String,
                        PosX: Int,
                        PosY: Int,
                        Width: Int,
                        Height: Int,
                        Category: String,
                        WaitingRoom: String,
                        BedOutsideRoom: String,
                        NumOccupants: Int,
                        Date: String,
                        Color: String,
                        Patients:List[EmergencyPatient]
                      ) extends JsonSerializable {
  def asJson(): JsValue = Json.toJson(this)

}

