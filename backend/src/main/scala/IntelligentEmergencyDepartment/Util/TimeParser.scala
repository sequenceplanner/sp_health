package IntelligentEmergencyDepartment.Util

import java.text.SimpleDateFormat
import java.time.LocalDateTime

import com.github.nscala_time.time.Imports._
import IntelligentEmergencyDepartment.Domain.Transformation.{ElvisEvent, ElvisPatient}
import IntelligentEmergencyDepartment.Config.LocaleTimeConfig
import org.joda.time.format.DateTimeFormatter

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}
import scala.util.{Failure, Success, Try}

object TimeParser {
  val ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  val externalFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
  val externalSpecialFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
  val ISOFormatter = new SimpleDateFormat(ISO8601)
  val timeFormat = new SimpleDateFormat("HH:mm")

  def now(): DateTime = DateTime.now(DateTimeZone.forID(LocaleTimeConfig.timeLocation))
  def timePassedSince(date: DateTime): Long = now().getMillis - date.getMillis

  val elvisEventDateFormatter: DateTimeFormatter = DateTimeFormat.forPattern(ElvisEvent.timeFormat)
  def parseElvisEventTime(time: String): Option[DateTime] = try {
    Some(DateTime.parse(time, elvisEventDateFormatter))
  } catch {
    case _: IllegalArgumentException =>
      println(s"parseElvisEventTime: Could not parse time $time")
      None
  }

  val elvisPublishedDateFormatter: DateTimeFormatter = DateTimeFormat.forPattern(ElvisPatient.timeFormat)
  val elvisPublishedDateFormatterBackUp: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
  def parseElvisPublishedTime(time: String): Option[DateTime] =
    Try(DateTime.parse(time, elvisPublishedDateFormatter))
      .orElse(Try(DateTime.parse(time, elvisPublishedDateFormatterBackUp))).toOption




  def toDatabaseDateFormat(date:DateTime): LocalDateTime = {
    LocalDateTime.of(date.getYear, date.getMonthOfYear, date.getDayOfMonth, date.getHourOfDay, date.getMinuteOfHour, date.getSecondOfMinute)
  }



  /**
   * Returns "hh:mm (day)"-format of argument date string in format yyyy-MM-dd'T'HH:mm:ssZ.
   */
  def toReadableTime(startTimeString: String): String = {
    Try {
      val startTime = externalFormatter.parse(startTimeString.replaceAll("Z$", "+0000"))
      val timeString = timeFormat.format(startTime)
      val diff = getTimeDiffExternalFormat(startTimeString)
      val days = MILLISECONDS.toDays(diff)

      val dayString = days match {
        case 0 => ""
        case 1 => s"(${LocaleTimeConfig.yesterday})"
        case n: Long => s"+$n d."
      }

      timeString + dayString
    } match {
      case Success(data) => data
      case Failure(_) => "N/A"
    }
  }

  def round(t: DateTime, d: FiniteDuration): DateTime = {
    t minus (t.getMillis - (t.getMillis.toDouble / d.toMillis).round * d.toMillis)
  }

  /**
  Returns the time difference (in milliseconds) between a given start time and now.
  Argument startTimeString must be received in date-time-format: yyyy-MM-ddTHH:mm:ss.SSSZ
   */
  def getTimeDiffExternalFormat(startTimeString: String): Long = {
    val now: Long = System.currentTimeMillis
    val startTime = Try(ISOFormatter.parse(startTimeString.replaceAll("Z$", "+0000")))

    startTime.fold(_ => 0, success => Math.abs(now - success.getTime))
  }
}
