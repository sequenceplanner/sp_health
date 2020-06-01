package IntelligentEmergencyDepartment.Stream

import IntelligentEmergencyDepartment.Database.{EmergencyEventRepository, EmergencyPatientEvent, PatientEvent}
import IntelligentEmergencyDepartment.Domain.Transformation.{Event, EmergencyPatientsOverview}
import IntelligentEmergencyDepartment.Util.PostManager
import scalaj.http.HttpResponse
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._
import scala.language.postfixOps
object Sources {

  private val emergencyEventsRepository = new EmergencyEventRepository()
  /**
   * Fetches events from db and combines them with the given overview. If the future fails recover is used to emit
   * a Failure before the stream is canceled
   * @param powerBiPatientsOverview the overview that are to be combined with the events
   * @param materializer used by Sink.head
   * @return
   */
  def getEventsWithRecover(powerBiPatientsOverview: EmergencyPatientsOverview, minutes:Int)(implicit materializer: ActorMaterializer): Future[(EmergencyPatientsOverview, Try[Seq[EmergencyPatientEvent]])] = {
    Source.fromFuture(emergencyEventsRepository.getEventsLast(minutes)).recover {
      case _ => Failure(new Throwable("Failed to fetch from db"))
    }
      .map(f => (powerBiPatientsOverview, f)).runWith(Sink.head)
  }

  /**
   * Attempts to send the given data to the given urls. If the future fails recover is used to emit
   * * a Failure before the stream is canceled
   *
   * @param urls the urls which the data should be sent to
   * @param data the data that should be sent
   * @param materializer used by Sink.head
   * @return
   */
  def sendLinkWithRecover(urls:List[String],data:List[JsValue])(implicit materializer: ActorMaterializer): Future[Seq[Try[HttpResponse[String]]]] = {
    Source.fromFuture(Future.sequence(urls.map(url => PostManager.postToPowerBi(url, data)))).recover {
      case _ =>
        println("Recover failed link!")
        Seq(Failure(new Throwable("Could not send data :/")))
    }
      .runWith(Sink.head[Seq[Try[HttpResponse[String]]]])
  }

  /**
   * Attempts to send the given data to the given link. Max 4 tries, if all fails a failure is returned.
   * @param link The link to send the data to
   * @param data The data that is to be sent
   * @param materializer  used by Sink.head
   * @return The resulting HttpResponse wrapped in a try.
   */
  def sendLinkWithRetry(link:String, data:List[JsValue])(implicit materializer: ActorMaterializer): Future[Try[HttpResponse[String]]] = {
    RestartSource.withBackoff(
      minBackoff = 10 milliseconds,
      maxBackoff =  3 seconds,
      randomFactor = 0.2,
      maxRestarts = 3
    ) { () =>
      val responseFuture: Future[Try[HttpResponse[String]]] =
        PostManager.postToPowerBi(link,data)
      Source.fromFuture(responseFuture)
        .map {{
          case Success(res) =>
            if (res.isSuccess) {
              Try(res)
            }
            else {
              println("Fail!")
              throw new Throwable("Error sending data")
            }
          case Failure(e) =>
            println("Fail! ")
            throw new Throwable("Exception sending data " + e.getMessage)
        }}
    }
      .runWith(Sink.head)
      .recover {
        case _ => Failure(new Throwable("Couldn't send data"))
      }
  }

}
