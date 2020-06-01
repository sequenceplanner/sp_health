package IntelligentEmergencyDepartment.Util

import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Paths}
import java.util.Base64

import scala.language.postfixOps
import akka.{Done, NotUsed}
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.googlecloud.pubsub._
import akka.stream.alpakka.googlecloud.pubsub.scaladsl.GooglePubSub
import akka.stream.scaladsl.{Flow, RestartSource, Sink, Source}
import IntelligentEmergencyDepartment.Util.Pubsub.withConfig

import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.duration._
import spray.json._
import spray.json.DefaultJsonProtocol._


/**
 * API that simplifies the usage of alpakka's Google PubSub bindings for scala.
 * Project id and other configuration settings are retrieved from the file
 * pointed to by the system env variable GOOGLE_APPLICATION_CREDENTIALS.
 *
 * @see https://github.com/akka/alpakka/blob/master/docs/src/main/paradox/google-cloud-pub-sub.md
 */
object Pubsub {
  var pubsubConfig: Option[PubSubConfig] = None
  val timeOut: FiniteDuration = 2 minutes

  def subscribeStream(subscription: String)(implicit system: ActorSystem, mat: ActorMaterializer): Source[PubSubMessage, Cancellable] = {
    withConfig { config =>
      val ackSink: Sink[AcknowledgeRequest, Future[Done]] = GooglePubSub.acknowledge(subscription, config)

      val subscribeMessageSource: Source[ReceivedMessage, Cancellable] = GooglePubSub.subscribe(subscription, config)
      val batchAckSink =
        Flow[ReceivedMessage].map(_.ackId).groupedWithin(10, 1.second).map(AcknowledgeRequest.apply).to(ackSink)
      // Flow[ReceivedMessage].map(_.ackId).map(x => AcknowledgeRequest(List(x))).to(ackSink)

      subscribeMessageSource
        .alsoTo(batchAckSink)
        .filter(m => {
          System.currentTimeMillis() - m.message.publishTime.toEpochMilli < timeOut.toMillis
        })
        .map(decodeReceivedMessage)
    }
  }

  /**
   * Decodes the contents of a message received from Google PubSub through the
   * alpakka interface
   *
   * @return A PubSubMessage where the contents are decoded.
   */
  def decodeReceivedMessage(message: ReceivedMessage): PubSubMessage = {
    val pubsubMessage = message.message
    val messageData = pubsubMessage.data
    if (messageData.isDefined) {
      val decodedData = new String(Base64.getDecoder.decode(pubsubMessage.data.get), StandardCharsets.UTF_8)
      pubsubMessage.withData(data = decodedData)
    } else {
      pubsubMessage
    }

  }


  def wrapInRestartSource[A](source: Source[A, _])(implicit system: ActorSystem, mat: ActorMaterializer): Source[A, NotUsed] = {
    RestartSource.withBackoff(
      minBackoff = 3.seconds,
      maxBackoff = 30.seconds,
      randomFactor = 0.2, // adds 20% "noise" to vary the intervals slightly
      maxRestarts = 20 // limits the amount of restarts to 20
    ) {
      () =>
        source
    }
  }

  def subscribe[A](subscription: String)(onReceiveMessage: PubSubMessage => Unit)(implicit system: ActorSystem, mat: ActorMaterializer): Future[Done] = {
    subscribeStream(subscription).runForeach(onReceiveMessage)
  }


  private def withConfig[A](f: PubSubConfig => A)(implicit system: ActorSystem): A = {
    // newConfig
    pubsubConfig.map(f) match {
      case Some(res) => res
      case None =>
        loadCredentials()
        f(pubsubConfig.get)
    }
  }

  /**
   * Load the Google-PubSub credential file. Should only be called once upon startup.
   */
  private def loadCredentials()(implicit system: ActorSystem): Unit = {
    val credentialsFile = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")

    new String(Files.readAllBytes(Paths.get(credentialsFile)), UTF_8)
    val credentialsData = Try(new String(Files.readAllBytes(Paths.get(credentialsFile))).parseJson.asJsObject().fields).toEither.left.map(_.getMessage)

    def getCredentialFor(credentials: Map[String, JsValue])(key: String) = {
      credentials
        .get(key)
        .map(_.convertTo[String].filterNot(c => c == '\"'))
        .toRight(s"Could not find JSON key '$key' in credential file at '$credentialsFile.'")
    }

    val loadedConfig = for {
      getCredential <- credentialsData.map(getCredentialFor)
      privateKey <- getCredential("private_key")
      clientEmail <- getCredential("client_email")
      projectId <- getCredential("project_id")
    } yield PubSubConfig(projectId, clientEmail, privateKey)

    pubsubConfig = loadedConfig.toOption

    loadedConfig match {
      case Left(err) => throw new CredentialException(err)
      case _ => ()
    }
  }

  class CredentialException(message: String) extends Exception(message) {
    def this(message: String, cause: Throwable) {
      this(message)
      initCause(cause)
    }

    def this(cause: Throwable) {
      this(Option(cause).map(_.toString).orNull, cause)
    }

    def this() {
      this(null: String)
    }
  }

  class PubsubConfigException(message: String) extends Exception(message) {
    def this(message: String, cause: Throwable) {
      this(message)
      initCause(cause)
    }

    def this(cause: Throwable) {
      this(Option(cause).map(_.toString).orNull, cause)
    }

    def this() {
      this(null: String)
    }
  }

}
