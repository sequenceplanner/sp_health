package IntelligentEmergencyDepartment.Database


import java.time.{Instant, LocalDateTime}

import slick.dbio.Effect
import slick.jdbc.SQLServerProfile.api._
import slick.lifted.ProvenShape
import slick.sql.FixedSqlAction
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Try

case class HospitalPatientEvent(careContactId: Int,
                                 CareEventId: Int,
                                 Category: String,
                                 Time:LocalDateTime,
                                 Title: String,
                                 Type: String,
                                 Value: String,
                                 VisitId: Int,
                                 PatientId:Int)
class HospitalEventRepository{
  val EventQuery: TableQuery[HospitalEventTable] = TableQuery[HospitalEventTable]

  def insert(event: HospitalPatientEvent): Future[Try[Int]] =
    Db.run(EventQuery += event)

  def insert(events:List[HospitalPatientEvent]): Future[Try[Option[Int]]] = {
    Db.runList(EventQuery ++= events)
  }

}
class HospitalEventTable(tag: Tag) extends Table[HospitalPatientEvent](tag, "HospitalEvents") {


  def careContactId: Rep[Int] = column[Int]("careContactId")
  def careEventId: Rep[Int] = column[Int]("careEventId")
  def category: Rep[String] = column[String]("category", SqlType("varchar(50)"))
  def time: Rep[LocalDateTime] = column[LocalDateTime]("date")
  def title: Rep[String] = column[String]("title", SqlType("varchar(50)"))
  def eventType: Rep[String] = column[String]("type", SqlType("varchar(50)"))
  def value:Rep[String] = column[String]("value", SqlType("varchar(50)"))
  def visitId:Rep[Int] = column[Int]("visitId")
  def patientId:Rep[Int] = column[Int]("patientId")

  def * : ProvenShape[HospitalPatientEvent] = (careContactId,careEventId,category, time,title,eventType,value,visitId,patientId) <>(HospitalPatientEvent.tupled, HospitalPatientEvent.unapply)
  def pk = primaryKey("pk_HospitalEvent", (patientId,category,time,eventType,value))
}





