package IntelligentEmergencyDepartment.Database

import java.time.{Instant, LocalDateTime}

import IntelligentEmergencyDepartment.Database.EventType.EventType
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.SQLServerProfile.api._
import slick.lifted.ProvenShape
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try

case class PandStatus(date:LocalDateTime, clinic:String, nrOfPand:Int)
class PandStatusRepository{
  val PatientQuery: TableQuery[PandStatusTable] = TableQuery[PandStatusTable]

  def insert(event: PandStatus): Future[Try[Int]] =
    Db.run(PatientQuery += event)

  def insert(events:List[PandStatus]): Future[Try[Option[Int]]] = {
    Db.runList(PatientQuery ++= events)
  }

}

class PandStatusTable(tag: Tag) extends Table[PandStatus](tag, "PandemiStatus") {

  def date: Rep[LocalDateTime] = column[LocalDateTime]("date")
  def clinic: Rep[String] = column[String]("clinic",SqlType("varchar(50)"))
  def nrOfPand: Rep[Int] = column[Int]("nrOfPand")



  def * : ProvenShape[PandStatus] = (date,clinic,nrOfPand) <>(PandStatus.tupled, PandStatus.unapply) // scalastyle:ignore
  def pk = primaryKey("pk_Pand_Status", (date,clinic))
}




