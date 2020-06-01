package IntelligentEmergencyDepartment.Database

import java.time.LocalDateTime

import IntelligentEmergencyDepartment.Domain.Transformation.EventDataTypes.TypeOfEvent
import IntelligentEmergencyDepartment.Util.TimeParser
import slick.dbio.Effect
import slick.dbio.Effect.All
import slick.jdbc
import slick.jdbc.SQLServerProfile
import slick.jdbc.SQLServerProfile.api._
import slick.lifted.ProvenShape
import slick.sql.FixedSqlAction
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.collection.compat.Factory
import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global


case class EmergencyPatientEvent(careContactId: Int,
                                 CareEventId: Int,
                                 Category: String,
                                 Time:LocalDateTime,
                                 Title: String,
                                 Type: String,
                                 Value: String,
                                 VisitId: Int,
                                 PatientId:Int)
class EmergencyEventRepository{
  val EventQuery: TableQuery[EmergencyEventTable] = TableQuery[EmergencyEventTable]

  def insert(event: EmergencyPatientEvent): Future[Try[Int]] =
    Db.run(EventQuery += event)

  def insert(events:List[EmergencyPatientEvent]): Future[Try[Option[Int]]] = {
    Db.runList(EventQuery ++= events)
  }

  def upsert(events:List[EmergencyPatientEvent]):Future[Try[List[Int]]] = {
    val toBeInserted: List[FixedSqlAction[Int, NoStream, Effect.Write]] = events.map { event => TableQuery[EmergencyEventTable].insertOrUpdate(event) }
    val inOneGo: DBIOAction[List[Int], NoStream, Effect.Write] = DBIO.sequence(toBeInserted)
    Db.run(inOneGo)
  }

  def getEventsLast(minutes:Int):Future[Try[Seq[EmergencyPatientEvent]]]= {
    val date = LocalDateTime.now().minusMinutes(minutes)


    val ids = EventQuery.filter(e=>e.time >= date).map(_.careContactId).distinct

    val innerJoin = for {
      (c, _) <- EventQuery join ids on (_.careContactId === _)
    } yield c
    Db.read(innerJoin.result)
  }
}
class EmergencyEventTable(tag: Tag) extends Table[EmergencyPatientEvent](tag, "Events3") {


  def careContactId: Rep[Int] = column[Int]("careContactId")
  def careEventId: Rep[Int] = column[Int]("careEventId")
  def category: Rep[String] = column[String]("category", SqlType("varchar(50)"))
  def time: Rep[LocalDateTime] = column[LocalDateTime]("date")
  def title: Rep[String] = column[String]("title", SqlType("varchar(50)"))
  def eventType: Rep[String] = column[String]("type", SqlType("varchar(50)"))
  def value:Rep[String] = column[String]("value", SqlType("varchar(50)"))
  def visitId:Rep[Int] = column[Int]("visitId")
  def patientId:Rep[Int] = column[Int]("patientId")

  def * : ProvenShape[EmergencyPatientEvent] = (careContactId,careEventId,category, time,title,eventType,value,visitId,patientId) <>(EmergencyPatientEvent.tupled, EmergencyPatientEvent.unapply)
  def pk = primaryKey("pk_emergencyEvent3", (careContactId,category,time,eventType,value))
}





