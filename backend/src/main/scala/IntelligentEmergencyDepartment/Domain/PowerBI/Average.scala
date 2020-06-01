package IntelligentEmergencyDepartment.Domain.PowerBI

import scala.collection.mutable.ArrayBuffer
import IntelligentEmergencyDepartment.Util.TimeParser
import com.github.nscala_time.time.Imports._
import IntelligentEmergencyDepartment.Domain.Transformation.EventDataTypes.TriageLevel
import IntelligentEmergencyDepartment.Domain.Transformation.EmergencyPatientsOverview

object Average{

  private var timeListTTT:ArrayBuffer[Long] = ArrayBuffer()    //This will contain all the Time To Triage
  private var tttAverage: Long = 0
  private val timeListTTL: List[ArrayBuffer[Long]] = List(ArrayBuffer(), //ME2
    ArrayBuffer(), //ME3
    ArrayBuffer(), //OR
    ArrayBuffer(), //KI
    ArrayBuffer()) //ME4JOUR
  private var oldOverviewTTT : EmergencyPatientsOverview = _  //Storing the old overview for comparison
  private var oldOverviewTTL : EmergencyPatientsOverview = _

  def calculateNewTTTAverage(p:EmergencyPatientsOverview):Long={
    if(oldOverviewTTT == null){  //Use this so that the code below doesn't generate an exception on the first itteration of the program
      oldOverviewTTT = p
      return 0
    }

    for(newpat <- p.patients){
      for(oldpat <- oldOverviewTTT.patients){
        if(newpat.visitId == oldpat.visitId && (oldpat.priority == TriageLevel.NotTriaged && newpat.priority != TriageLevel.NotTriaged)){ //if a patient has changed the triage level, he/she is no longer in waiting
          timeListTTT += (TimeParser.now().getMillis - timeToLong(newpat.visitRegistrationTime) - newpat.latestEventTimeDiff)/60000
          //time in min
        }
      }
    }
    oldOverviewTTT = p //update the old view
    var sum:Long = 0
    if(timeListTTT.length > 10) timeListTTT.remove(0) //only use the latest 10 values to get an up to date estimation
    for(i <- timeListTTT){ //sum together all the values
      sum += i
    }
    if(timeListTTT.length != 0){   //avoiding a divide by zero exception
      tttAverage = sum/timeListTTT.length //divide by length to get Average
    }
    tttAverage
  }

  def calculateNewTTLAverage(p:EmergencyPatientsOverview):ArrayBuffer[Long]={
    if(oldOverviewTTL == null){  //Use this so that the code below doesn't generate an exception on the first itteration of the program
      oldOverviewTTL = p
      return ArrayBuffer(0,0,0,0,0)
    }

    for(newpat <- p.patients){
      for(oldpat <- oldOverviewTTL.patients){
        if(newpat.visitId == oldpat.visitId && (oldpat.doctorId == "N/A" && newpat.doctorId != "N/A")){

          newpat.clinic.commonName match { //If a patient has been apointed a doctor, he/she is sorted based on the department
            case "ME2" => timeListTTL(0) += (TimeParser.now().getMillis - timeToLong(newpat.visitRegistrationTime) - newpat.latestEventTimeDiff)/60000
            case "ME3" => timeListTTL(1) += (TimeParser.now().getMillis - timeToLong(newpat.visitRegistrationTime) - newpat.latestEventTimeDiff)/60000
            case "OR"  => timeListTTL(2) += (TimeParser.now().getMillis - timeToLong(newpat.visitRegistrationTime) - newpat.latestEventTimeDiff)/60000
            case "KI"  => timeListTTL(3) += (TimeParser.now().getMillis - timeToLong(newpat.visitRegistrationTime) - newpat.latestEventTimeDiff)/60000
            case "ME4" | "LILA" => timeListTTL(4) += (TimeParser.now().getMillis - timeToLong(newpat.visitRegistrationTime) - newpat.latestEventTimeDiff)/60000
            case _ =>
          }
        }
      }
    }
    oldOverviewTTL = p //update the old view
    val sums: ArrayBuffer[Long] = ArrayBuffer(0, 0, 0, 0, 0)
    for(i <- 0 to timeListTTL.length - 1){ //sum together all the values
      if(timeListTTL(i).length > 10) timeListTTL(i).remove(0) //only use the latest 10 values to get an up to date estimation
      for(j <- timeListTTL(i)){
        sums(i) = sums(i)+j
      }
      if(timeListTTL(i).length != 0){   //avoiding a divide by zero exception
        val average = sums(i)/timeListTTL(i).length - tttAverage //divide by length to get Average
        if(average >= 0) sums(i) = average
        else sums(i) = 0
      }
    }
    sums //return the average TTL values
  }

  def timeToLong(date:String):Long={  //parsing the string of the time to the using a function from TimeParser

    val newDate : DateTime = TimeParser.parseElvisEventTime(date).get
    newDate.getMillis

  }
}
