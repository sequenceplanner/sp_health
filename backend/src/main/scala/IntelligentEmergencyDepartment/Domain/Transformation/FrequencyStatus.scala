package IntelligentEmergencyDepartment.Domain.Transformation

case class FrequencyStatus(dep: Department, unAttendedChange: Int, attendedChange: Int, finishedChange: Int)

object FrequencyStatus {
  def calculateChanges(dep: Department, nrOfUnAttendedEvents: Int, nrOfAttendedEvents: Int, nrOfFinishedEvents: Int, nrOfLeftAsFinished: Int)
  = FrequencyStatus(dep,
    nrOfUnAttendedEvents - nrOfAttendedEvents,
    nrOfAttendedEvents - nrOfFinishedEvents,
    nrOfFinishedEvents - nrOfLeftAsFinished)
}
