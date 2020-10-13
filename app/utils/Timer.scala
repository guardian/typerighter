package utils

import scala.collection.JavaConverters._
import play.api.Logging
import net.logstash.logback.marker.Markers
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

object Timer extends Logging {
  /**
    * Time a synchronous task and log the results. Warns if the
    */
  def time[R](taskName: String, additionalMarkers: Map[String, String] = Map.empty[String, String])(block: => R): R = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    logTime(taskName, t0, t1, additionalMarkers)
    result
  }

  /**
    * Time an asynchronous task returning a Future, and log the results.
    */
  def timeAsync[R](taskName: String, additionalMarkers: Map[String, String] = Map.empty[String, String])(block: => Future[R])(implicit ec: ExecutionContext): Future[R] = {
    val t0 = System.nanoTime()
    block.map { result =>
      val t1 = System.nanoTime()
      logTime(taskName, t0, t1, additionalMarkers)
      result
    }
  }

  private def logTime(taskName: String, fromInNs: Long, toInNs: Long, additionalMarkers: Map[String, String]) = {
    val durationInMs = (toInNs - fromInNs) / 1000000
    val markers = Markers.appendEntries((
      Map(
        "taskName" -> taskName,
        "durationInMs" -> durationInMs
      ) ++ additionalMarkers
    ).asJava)

    val message = s"Task $taskName complete in $durationInMs"
    logger.info(message)(markers)
  }
}
