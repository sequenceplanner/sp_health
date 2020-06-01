package IntelligentEmergencyDepartment.Stream.Flows

import IntelligentEmergencyDepartment.Domain.JsonSerializable
import IntelligentEmergencyDepartment.Domain.Transformation.EventDataTypes.{Category, TypeOfEvent, Title}
import IntelligentEmergencyDepartment.Domain.Transformation._
import IntelligentEmergencyDepartment.Stream.{Sinks, Sources}
import IntelligentEmergencyDepartment.Util.{ChangeTimeZone, Converter, EventFinder, TimeParser}
import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.googlecloud.pubsub.PubSubMessage
import akka.stream.scaladsl.{Flow, Sink}
import org.joda.time.DateTime
import scalaj.http.HttpResponse

import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

object GeneralFlows {


  val excludeOldMessages: Flow[PubSubMessage, PubSubMessage, NotUsed] = Flow[PubSubMessage].statefulMapConcat { () =>
    var lastPubSubMessage: PubSubMessage = null
    newMessage => {
      if (lastPubSubMessage == null) {
        lastPubSubMessage = newMessage
        List(None)
      }
      else if (newMessage.publishTime.isAfter(lastPubSubMessage.publishTime)) {
        lastPubSubMessage = newMessage
        List(Some(newMessage))
      } else {
        List(None)
      }
    }
  }.collect({ case Some(message) => message })

  /**
   * Sends a list of JsonSerializable to the given links
   *
   * @param links the links the data should be sent to
   * @param mat   used by inner method
   * @return
   */
  def sendSerializable(links: List[String])(implicit mat: ActorMaterializer): Flow[List[JsonSerializable], Seq[Try[HttpResponse[String]]], NotUsed] =
    Flow[List[JsonSerializable]].map(_.map(_.asJson()))
      .mapAsync(1)(
        unitOverviewsAsJson => Sources.sendLinkWithRecover(links, unitOverviewsAsJson))

  def sendSerializableWithRetry(links: List[String])(implicit mat: ActorMaterializer): Flow[List[JsonSerializable], Seq[Try[HttpResponse[String]]], NotUsed] =
    Flow[List[JsonSerializable]].map(_.map(_.asJson()))
      .mapAsync(1)(
        unitOverviewsAsJson => Future.sequence(links.map(link => Sources.sendLinkWithRetry(link, unitOverviewsAsJson))))

  def sendSerializableItem(links: List[String])(implicit mat: ActorMaterializer): Flow[JsonSerializable, Seq[Try[HttpResponse[String]]], NotUsed] =
    Flow[JsonSerializable].map(_.asJson())
      .mapAsync(1)(
        unitOverviewsAsJson => Sources.sendLinkWithRecover(links, List(unitOverviewsAsJson)))


  /**
   * A genereal flow for checking if something is different from last time
   */
  def diffFlow[A](name: String, checkDiff: A => Boolean): Flow[A, A, NotUsed] = Flow[A].statefulMapConcat(() => {
    var lastSentDate = System.currentTimeMillis()
    items => {
      val timeNow = System.currentTimeMillis()
      val moreThanTenMinutes = timeNow - lastSentDate > 600000
      val diff = checkDiff(items) || moreThanTenMinutes
      if (diff) {
        println("Diff in " + name)
        lastSentDate = timeNow
        List(Some(items))
      }
      else {
        println("No diff in " + name)
        List(None)
      }
    }
  }).collect({ case Some(message) => message })

  /**
   * Converts an elvis patient overview to an emergency patient overview
   */
  val convertToEmergencyPatientOverview: Flow[ElvisPatientsOverview, EmergencyPatientsOverview, NotUsed] =
    Flow[ElvisPatientsOverview].map(Converter.convertToEmergencyOverview)

  /**
   * Picks out the events that differs from the last message that was received. Returns a tuple with the overview and
   * the event that differs.
   */
  val diffNewEventsFlow: Flow[EmergencyPatientsOverview, (EmergencyPatientsOverview, List[EmergencyEventsHolder]), NotUsed] =
    Flow[EmergencyPatientsOverview].statefulMapConcat { () =>
      var oldPatients: List[EmergencyPatient] = List()
      newOverview => {

        //Compare new patients with old ones
        var newEvents = newOverview.patients.map(newPat => {
          oldPatients.find(_.careContactId == newPat.careContactId) match {
            //Old patient exists, diff between events to find new ones
            case Some(oldPat) => EmergencyEventsHolder(newPat.careContactId, newPat.clinic, newPat.events diff oldPat.events)
            //No old patient exists, all events are new
            case None => EmergencyEventsHolder(newPat.careContactId, newPat.clinic, newPat.events)
          }
        })
        //Any patient that has been removed?
        newEvents = newEvents ++ oldPatients.filterNot(oldPat =>
          newOverview.patients.exists(_.careContactId == oldPat.careContactId))
          .map(oldPat =>
            EmergencyEventsHolder(oldPat.careContactId, oldPat.clinic,
              List(Event(oldPat.careContactId, 0, Some(Category.Custom), Title.RemovedPatient, TypeOfEvent.RemovedPatient.name, "", oldPat.visitId, oldPat.visitId, TimeParser.parseElvisPublishedTime(newOverview.date).get))))
        oldPatients = newOverview.patients

        List((newOverview, newEvents))
      }
    }

  val filterCorona: Flow[ElvisPatientsOverview, (DateTime, List[ElvisPatient]), NotUsed] =
    Flow[ElvisPatientsOverview].map(epo => {
      (TimeParser.parseElvisPublishedTime(epo.date), epo.patients.filter(_.reasonForVisit.toLowerCase == "pand"))
    }).collect { case (Some(time), patients) => (ChangeTimeZone.changeToSwedishTimeZone(time), patients) }




  val getPatients: Flow[ElvisPatientsOverview, List[ElvisPatient], NotUsed] =
    Flow[ElvisPatientsOverview].map(epo => epo.patients)


  //Appends all custom events to the patients
  val appendCustomEvents: Flow[EmergencyPatientsOverview, EmergencyPatientsOverview, NotUsed] = Flow[EmergencyPatientsOverview].statefulMapConcat { () =>
    var oldPatients: List[(EmergencyPatient, List[Event])] = List()
    var firstTime = true
    overview =>
      val date = TimeParser.parseElvisPublishedTime(overview.date).getOrElse(DateTime.now())
      val toReturn: List[(EmergencyPatient, List[Event])] = overview.patients.map(newPat => {
        oldPatients.find(_._1.careContactId == newPat.careContactId) match {
          case Some(oldPat) =>
            (newPat, oldPat._2 ++ EventFinder.createCustomEventsForEmergencyPatient(date, newPat, oldPat._1))
          case None => (newPat, EventFinder.createCustomEventsForNewEmergencyPatient(newPat.getTimeOfFirstEvent.getOrElse(date), newPat))
        }
      })

      firstTime = false
      oldPatients = toReturn
      List(EmergencyPatientsOverview(toReturn.map { case (a, b) => a.copy(events = a.events ++ b) }, overview.date))

  }
}
