package IntelligentEmergencyDepartment.Stream

import akka.stream.scaladsl.Sink
import IntelligentEmergencyDepartment.Config.Values

import IntelligentEmergencyDepartment.Domain.PowerBI.{Average, Waitingroom}
import IntelligentEmergencyDepartment.Util.{FilterLocation, PostManager, ChangeTimeZone}
import play.api.libs.json.Json
import IntelligentEmergencyDepartment.Domain.Transformation.{ElvisPatientsOverview, Event, EmergencyPatientsOverview}

import scala.concurrent.Future
import akka.Done
import scalaj.http.HttpResponse

import scala.util.{Failure, Success, Try}


object Sinks{

  val printID: Sink[EmergencyPatientsOverview, Future[Done]] = Sink.foreach {
    p => {
      println("Emergency Department status: Number of patients: " + p.patients.length)

    }
  }

  val printHospitalNewEvent: Sink[ElvisPatientsOverview, Future[Done]] = Sink.foreach {
    p => {
      println("Received Event: " + p.date)
    }
  }


  /**
   * Determines whether the http responses are successful or not.
   * @param setSuccess the function which will be used to set whether all of the http responses were successful
   * @return
   */
  def handleHttpResponse(setSuccess: Boolean => Unit): Sink[Seq[Try[HttpResponse[String]]], Future[Done]] = Sink.foreach {
    responses =>
      if (responses.map {
        case Success(res) =>
          if (res.isSuccess) {
            println("Post was succesful")
            true
          } else {
            println("Post was not succesful")
            false
          }
        case Failure(e) =>
          println("Post failed with exception " + e.getMessage)
          false
      }.forall(_ == true)) {
        setSuccess(true)
      } else {
        setSuccess(false)

      }
  }


  def handleDbResponses(tag:String): Sink[List[Try[Int]], Future[Done]] = Sink.foreach(list=>{
    println(
      tag + " Number of rows affected by db: " +
      list.map({
      case Success(number) => number
      case Failure(e) => println("Failed to insertToDb " + e.getMessage)
                          0
    }).sum)
  })

  def handleOptionalDbResponse(tag:String): Sink[Try[Option[Int]], Future[Done]] = Sink.foreach(list=>{
    println(
      tag + " Number of rows affected by db: " +
        (list match {
          case Success(Some(number)) => number
          case Success(_) => 0
          case Failure(e) => println("Failed to insertToDb " + e.getMessage)
            0
        }))
  })


  def handleDBResponseWithList(tag:String): Sink[Try[List[Int]], Future[Done]] = Sink.foreach(response => {
    println(
      tag + " Number of rows affected by db: " +
        (response match{
          case Success(n) => n.sum
          case Failure(e) => println("Failed to insertToDb " + e.getMessage)
            0
        }))
  })


  def handleDbResponse(tag:String): Sink[Try[Int], Future[Done]] = Sink.foreach(number=>{
    println(
      tag + " Number of rows affected by db: " +
        (number match{
      case Success(n) => n
      case Failure(e) => println("Failed to insertToDb " + e.getMessage)
                            0
    }))
  })

}
