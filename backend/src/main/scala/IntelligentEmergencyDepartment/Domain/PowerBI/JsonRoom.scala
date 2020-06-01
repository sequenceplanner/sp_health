package IntelligentEmergencyDepartment.Domain.PowerBI

import play.api.libs.json.{Format, JsValue, Json, Reads}


object JsonRoom {
  implicit val JsonRoomEvent: Format[JsonRoom] = Json.format[JsonRoom]
  implicit val JsonRoomsRead: Reads[List[JsonRoom]] = Reads.list(JsonRoomEvent)
  var possible_rooms: List[JsonRoom] = List()

  def initPossibleRooms( rooms:List[JsonRoom]):Unit = {
      possible_rooms = rooms
  }
}

case class JsonRoom(    
                      name: String,
                      variations: List[String],
                      category: List[String],
                      posX:Int,
                      posY:Int,
                      width:Int,
                      height:Int,
                      color:String,
                      waitingRoom: Boolean,
                      bedOutsideRoom:Boolean,
                      prioritizedCategory:Boolean
                
                      
                      ){def asJson(): JsValue = Json.toJson(this)

}