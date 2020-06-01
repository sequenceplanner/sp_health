
package IntelligentEmergencyDepartment.Util

import IntelligentEmergencyDepartment.Domain.PowerBI.{JsonRoom, PowerBiRoom}
import IntelligentEmergencyDepartment.Domain.Transformation.{EmergencyPatient, EmergencyPatientsOverview}




object FilterLocation {
  def filter(p: EmergencyPatientsOverview, r:List[JsonRoom]): List[PowerBiRoom] ={

    p.patients.filterNot(l=>r.exists(f=>f.variations.contains(l.location.toLowerCase().filterNot((x: Char) => x.isWhitespace)))).foreach(s=>println("Room not in json list " + s.location))

    val date = ChangeTimeZone.changeToSwedishTime(p.date)
    var final_list: List[PowerBiRoom] = List()

    r.foreach(current_room=>{
      var numOc = 0
      var patientList: List[EmergencyPatient] = List()
      p.patients.foreach(pat=>{

      val clean_location = pat.location.toLowerCase().filterNot((x:Char) => x.isWhitespace)
        if(current_room.variations.contains(clean_location)){ 

          val department: String = pat.clinic.name.toLowerCase()

          
          if(current_room.waitingRoom && current_room.category.contains(department)){
              patientList = pat :: patientList
              numOc += 1
          }

          else if(current_room.bedOutsideRoom && current_room.category.contains(department)){
            patientList = pat :: patientList
            numOc += 1
          }

          else if(current_room.prioritizedCategory){
            patientList = pat :: patientList
              numOc+=1
          }

          else if(!current_room.waitingRoom) // If the patietns location is not a waiting room add it to its location
            patientList = pat :: patientList
            numOc += 1
          }
       })

       final_list = PowerBiRoom(current_room.name,current_room.posX,current_room.posY,current_room.width,current_room.height,current_room.category.head,current_room.waitingRoom.toString,current_room.bedOutsideRoom.toString,patientList.length,date,current_room.color,patientList)::final_list

      })
    final_list
  }






}
