package IntelligentEmergencyDepartment.Stream

import IntelligentEmergencyDepartment.Domain.PowerBI.{UnitOverview, DoctorPatient, PandOverview,PowerBiRoom,Waitingroom,InnerWaitingroom}

object Diff {



  private var oldDepartments: List[UnitOverview] = List()
  private var old_rooms:List[PowerBiRoom] = List()
  private var old_corona_rooms:List[PowerBiRoom] = List()
  private var oldPandOverviews: List[PandOverview] = List()

   private var oldWaiting = 0; //for waitingroomDiff
   private var oldTriaged = 0; //for waitingroomDiff

   private var oldInnerWaiting:List[Long] = List(0,0,0,0,0); //for InnerwaitingroomDiff
   private var oldInnerTreated:List[Long] = List(0,0,0,0,0); //for InnerwaitingroomDiff

   private var oldDoctorPatient:List[DoctorPatient] = List() ////for patientDoctorDiff

   /**
    * Checks if the given list of departments overview differs from the last one
    * @param newDepartments The new department overviews
    * @return true if differs, false otherwise
    */
   def departmentsDiff(newDepartments : List[UnitOverview]): Boolean = {
     val stillSame = !compareDepartmentLists(newDepartments,oldDepartments)
     oldDepartments = newDepartments
     stillSame
   }

  @scala.annotation.tailrec
  private def removeFromList(oldDepartments: List[UnitOverview], dOverview: UnitOverview): List[UnitOverview] = {
    oldDepartments match {
      case x :: xs if x == dOverview => xs
      case _ :: xs => removeFromList(xs, dOverview)
      case _ => oldDepartments
    }
  }

  /**
   * Checks whether the given lists have the same departments and that departments have the
   * same statistics
   *
   * @param newDepartments the new departments
   * @param oldDepartments the old departments
   * @return true if same false otherwise
   */
  @scala.annotation.tailrec
  private def compareDepartmentLists(newDepartments: List[UnitOverview], oldDepartments: List[UnitOverview]): Boolean = {
    newDepartments match {
      case x :: xs =>
        oldDepartments.find(p => p.sameStatistics(x)) match {
          case Some(dOverview) => compareDepartmentLists(xs, removeFromList(oldDepartments, dOverview))
          case None => false
        }
      case _ => oldDepartments.isEmpty
    }
  }

// ----- Compares the number of triaged and the number waiting with the old values, if therer is a difference: returns true -----
   def waitingroomDiff(p : Waitingroom):Boolean={
     val newWaiting = p.nrOfWaiting
     val newTriaged = p.nrOfTriaged

     if(newTriaged == oldTriaged && newWaiting == oldWaiting)
        false
     else{
        oldTriaged = newTriaged
        oldWaiting = newWaiting
        true
      }
   }

// ----- Compares the number if people in the inner waitingrooms and the number of treated patients, if therer is a difference: returns true --------
  def innerWaitingroomDiff(p : List[InnerWaitingroom]):Boolean={
    val newInnerWaiting: List[Long] = List(p(0).waiting, p(1).waiting, p(2).waiting, p(3).waiting, p(4).waiting)
    val newInnerTreated: List[Long] = List(p(0).treated, p(1).treated, p(2).treated, p(3).treated, p(4).treated)
    for(i <- 0 to 4){
      if(newInnerWaiting(i) != oldInnerWaiting(i) || newInnerTreated(i) != oldInnerTreated(i)){
        oldInnerTreated = newInnerTreated
        oldInnerWaiting = newInnerWaiting
        true
      }
    }
    false
  }

   def roomsDiff(new_rooms:List[PowerBiRoom]):Boolean ={
    var new_diff = false

    if(old_rooms.length==0){
      new_diff = true
    }

    else{
      for(i<-0 to new_rooms.length - 1){
        if(new_rooms(i).NumOccupants != old_rooms(i).NumOccupants){
            new_diff = true
        }
      }
    }
    old_rooms = new_rooms
    new_diff
   }

   def roomsCoronaDiff(new_rooms:List[PowerBiRoom]):Boolean ={
    var new_diff = false

    if(old_corona_rooms.length==0){
      new_diff = true
    }

    else{
      for(i<-0 to new_rooms.length - 1){
        if(new_rooms(i).NumOccupants != old_corona_rooms(i).NumOccupants){
            new_diff = true
        }
      }
    }
    old_corona_rooms = new_rooms
    new_diff
   }





  def pandStatusDiff(newPandOverviews: List[PandOverview]): Boolean = {
    val diff = !newPandOverviews.forall(p=>oldPandOverviews.exists(p2=>p2.Department == p.Department && p2.NumberOFPand==p.NumberOFPand)) ||
      !oldPandOverviews.forall(p=>newPandOverviews.exists(p2=>p2.Department == p.Department && p2.NumberOFPand==p.NumberOFPand))

    oldPandOverviews = newPandOverviews
    diff
  }


// ---- first checks if the new list of doctors with patients is the same as before, if that is the case: Checks that every patient and doctor has the same spot as before in the list -----
// ---- if therer is a difference: returns true ------
   def patientDoctorDiff(newDoctorPatient: List[DoctorPatient]):Boolean={

     if(newDoctorPatient.length != oldDoctorPatient.length){
       oldDoctorPatient = newDoctorPatient
       return true
     }
     for(i <- 0 to newDoctorPatient.length - 1){
       if(oldDoctorPatient(i).patient != newDoctorPatient(i).patient || oldDoctorPatient(i).doctor != newDoctorPatient(i).doctor){
         oldDoctorPatient = newDoctorPatient
         return true
       }
     }
     false
   }
 }
