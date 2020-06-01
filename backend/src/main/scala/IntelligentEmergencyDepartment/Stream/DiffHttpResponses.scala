package IntelligentEmergencyDepartment.Stream

object DiffHttpResponses {

  private var unitOverviewSendSuccess = false
  private var pandOverviewLastPostStatus = false
  private var roomsLastPostStatus = false
  private var docPatientsLastPostStatus = false
  private var patientViewLastPostStatus = false
  private var coronaRoomsLastPostStatus = false

  def setUnitOverviewLastPostStatus(status:Boolean): Unit ={
    println("Setting unit overview send to " + status)
    unitOverviewSendSuccess=status
  }
  def getUnitOverviewLastPostStatus: Boolean = {
    unitOverviewSendSuccess
  }

  def setPandOverviewLastPostStatus(status:Boolean): Unit = {
    println("Setting pandstatus overview send to " + status)
    pandOverviewLastPostStatus = status
  }
  def getPandOverviewLastPostStatus:Boolean ={
    pandOverviewLastPostStatus
  }

  def setRoomsLastPostStatus(status:Boolean): Unit = {
    println("Setting roomstatus overview send to " + status)
    roomsLastPostStatus = status
  }
  def getRoomsLastPostStatus:Boolean ={
    roomsLastPostStatus
  }

  def setDocPatientsLastPostStatus(status:Boolean): Unit = {
    println("Setting DocPatients send to " + status)
    docPatientsLastPostStatus = status
  }
  def getDocPatientsLastPostStatus:Boolean ={
    docPatientsLastPostStatus
  }

  def setWaitingroomLastPostStatus(status:Boolean): Unit = {
    println("Setting Waitingroom send to " + status)
    patientViewLastPostStatus = status
  }
  def getWaitingroomLastPostStatus:Boolean ={
    patientViewLastPostStatus
  }

  def setInnerWaitingroomLastPostStatus(status:Boolean): Unit = {
    println("Setting InnerWaitingroom send to " + status)
    patientViewLastPostStatus = status
  }
  def getInnerWaitingroomLastPostStatus:Boolean ={
    patientViewLastPostStatus
  }

  def setCoronaRoomsLastPostStatus(status:Boolean): Unit = {
    println("Setting Corona roomstatus overview send to " + status)
    coronaRoomsLastPostStatus = status
  }
  def getCoronaRoomsLastPostStatus:Boolean ={
    coronaRoomsLastPostStatus
  }

}
