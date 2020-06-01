package IntelligentEmergencyDepartment.Domain.Transformation

import play.api.libs.json.{Format, Json, Reads}

case class Department(name:String, commonName: String)
object Department {
  var possibleDepartments:List[Department] = List()

  def initPossibleDepartments(departments:List[Department]): Unit ={
    possibleDepartments=departments
  }

  def getDepartment(name:String): Option[Department] = {
     possibleDepartments.find(p=>p.name==name)
  }

  implicit val departmentFormat: Format[Department] = Json.format[Department]
  implicit val departmentsRead: Reads[List[Department]] = Reads.list(departmentFormat)



}
