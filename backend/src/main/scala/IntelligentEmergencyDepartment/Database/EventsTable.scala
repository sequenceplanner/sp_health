package IntelligentEmergencyDepartment.Database

import java.time.Instant

import IntelligentEmergencyDepartment.Database.EventType.EventType
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcType, SQLServerProfile}
import slick.jdbc.SQLServerProfile.api._
import slick.lifted.ProvenShape

import scala.concurrent.Future
import scala.util.Try

case class PatientEvent(date:Instant, careContactId:Int, clinic:String, doctorID:String, eventType:EventType)
class PatientEventRepository{
  val PatientQuery: TableQuery[PatientTable] = TableQuery[PatientTable]

  def insert(event: PatientEvent): Future[Try[Int]] =
    Db.run(PatientQuery += event)


  def getEventsLast(minutes:Int):Future[Try[Seq[PatientEvent]]] = {
    val time = Instant.now().minusSeconds(minutes*60)
    val query = PatientQuery.filter(p=>p.date >= time )
    Db.read(query.result)
  }
}

class PatientTable(tag: Tag) extends Table[PatientEvent](tag, "PatientEvents") {

  def date: Rep[Instant] = column[Instant]("date")
  def careContactId: Rep[Int] = column[Int]("careContactId")
  def clinic: Rep[String] = column[String]("clinic")
  def doctorID: Rep[String] = column[String]("doctorID")
  def eventType: Rep[EventType] = column[EventType]("eventType")


  def * : ProvenShape[PatientEvent] = (date,careContactId,clinic,doctorID, eventType) <>(PatientEvent.tupled, PatientEvent.unapply) // scalastyle:ignore
  def pk = primaryKey("pk_Patient", (careContactId,date))
}

object EventType extends Enumeration {
  type EventType = Value
  val UNATTENDED,ATTENDED,FINISHED,LEFT_AS_FINISHED = Value

  implicit val enumMapper: JdbcType[EventType] with BaseTypedType[EventType] = MappedColumnType.base[EventType, String](
    e => e.toString,
    s => EventType.withName(s)
  )
}


