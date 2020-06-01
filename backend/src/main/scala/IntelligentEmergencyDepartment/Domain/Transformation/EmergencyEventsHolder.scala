package IntelligentEmergencyDepartment.Domain.Transformation

/**
 * Contains a list with events that belongs to the patient with the careContactId
 * @param careContactId
 * @param department
 * @param newEvents
 */
case class EmergencyEventsHolder(careContactId:Int, department: Department, newEvents:List[Event])
