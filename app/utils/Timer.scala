package utils

import scala.collection.JavaConverters._
import play.api.Logging
import net.logstash.logback.marker.Markers
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import net.logstash.logback.marker.LogstashMarker

object Timer extends Logging {
  /**
    * Time a synchronous task and log the results.
    */
  def time[R](taskName: String, additionalMarkers: LogstashMarker)(block: => R): R = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    logTime(taskName, t0, t1, additionalMarkers)
    result
  }

  /**
    * Time an asynchronous task returning a Future, and log the results.
    */
  def timeAsync[R](taskName: String, additionalMarkers: LogstashMarker)(block: => Future[R])(implicit ec: ExecutionContext): Future[R] = {
    val t0 = System.nanoTime()
    block.map { result =>
      val t1 = System.nanoTime()
      logTime(taskName, t0, t1, additionalMarkers)
      result
    }
  }

  private def logTime(taskName: String, fromInNs: Long, toInNs: Long, additionalMarkers: LogstashMarker) = {
    val durationInMs = (toInNs - fromInNs) / 1000000
    val markers = Markers.appendEntries((
      Map(
        "taskName" -> taskName,
        "durationInMs" -> durationInMs
      )
    ).asJava)
    markers.add(additionalMarkers)

    val message = s"Task $taskName complete in ${durationInMs}ms"
    logger.info(message)(markers)
  }
}
