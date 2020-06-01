package IntelligentEmergencyDepartment.Database


import slick.basic.DatabaseConfig
import slick.dbio.Effect
import slick.jdbc.JdbcProfile
import slick.jdbc.SQLServerProfile.api._
import slick.sql.{FixedSqlAction, FixedSqlStreamingAction}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global


object Db {



  val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("sqlserver")
  val db: JdbcProfile#Backend#Database = dbConfig.db

  private var can_connect = false

  def initDatabase(): Unit = {
    println("Checking the database connection")
    tryConnect()
    if (can_connect) {
      println("Creating database tables if needed")
      import scala.concurrent.ExecutionContext.Implicits.global

      val shouldExistTables = List(TableQuery[PandStatusTable], TableQuery[EmergencyEventTable],TableQuery[HospitalEventTable])
      val existingTables = Await.result(db.run(dbConfig.profile.defaultTables), 10.seconds).toList

      val existingTableNames = existingTables.map(mt => mt.name.name)

      // Creating tables that
      shouldExistTables.filter(table => !existingTableNames.contains(table.baseTableRow.tableName))
        .map(_.schema.create).foreach(schema => Await.result(db.run(schema), 3.seconds))

      println("Database tables created!")

    } else {
      println("Could not establish connection with the database. Will not write to the database during this run!")
    }
  }


  def run(value: FixedSqlAction[Int, NoStream, Effect.Write]): Future[Try[Int]] = {
    if (can_connect) {
      db.run(value.asTry)
    } else {
      Future.failed(new Throwable("Cant connect"))
    }

  }



  def runList(value: FixedSqlAction[Option[Int], NoStream, Effect.Write]) = {
    if (can_connect) {
      db.run(value.asTry)
    } else {
      Future.failed(new Throwable("Cant connect"))
    }

  }


  def run(value: DBIOAction[List[Int], NoStream, Effect.Write]) = {
    if (can_connect) {
      db.run(value.asTry)
    } else {
      Future.failed(new Throwable("Cant connect"))
    }
  }



  def read(value: FixedSqlAction[Int, NoStream, Effect.Read]): Future[Try[Int]] = {
    if (can_connect) {
      db.run(value.asTry)
    } else {
      Future{Failure(new Throwable("Not Connected to db"))}
    }

  }

  def read[A](value: FixedSqlStreamingAction[Seq[A], A, Effect.Read]): Future[Try[Seq[A]]] = {
    if (can_connect) {
      db.run(value.asTry)
    } else {
      Future{Failure(new Throwable("Not connected to db"))}
    }
  }




  private def tryConnect(): Unit = Try(db.createSession.conn) match {
    case Success(con) =>
      con.close()
      can_connect = true
    case fail => println(fail.failed.get.printStackTrace())
  }


  def close(): Unit = {
    db.close()
  }

}
