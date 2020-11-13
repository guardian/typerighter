package utils

import scala.collection.JavaConverters._
import play.api.Logging
import net.logstash.logback.marker.Markers
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import net.logstash.logback.marker.LogstashMarker

/**
  * A collection of methods to help time synchronous and asynchronous operations.
  *
  * Logs the results with the Play logger.
  */
object Timer extends Logging {
  /**
    * Time a synchronous task and log the results.
    *
    * @param taskName the name of the task, which is added to the written log
    * @param additionalMarkers any additional markers to add to the log entry
    * @param slowLogThresholdMs pass to log at `warn` level when the operation exceeds the threshold.
    * @param onSlowLog evaluated when the operation exceeds the slow log threshold
    */
  def time[R](taskName: String, additionalMarkers: LogstashMarker = Markers.empty(), slowLogThresholdMs: Int = Int.MaxValue, onSlowLog: => Unit = {})(block: => R): R = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    logTime(taskName, t0, t1, additionalMarkers, slowLogThresholdMs, onSlowLog)
    result
  }

  /**
    * Time an asynchronous task returning a Future, and log the results.
    *
    * @param taskName the name of the task, which is added to the written log
    * @param additionalMarkers any additional markers to add to the log entry
    * @param slowLogThresholdMs pass to log at `warn` level when the operation exceeds the threshold.
    * @param onSlowLog evaluated when the operation exceeds the slow log threshold
    */
  def timeAsync[R](taskName: String, additionalMarkers: LogstashMarker = Markers.empty(), slowLogThresholdMs: Int = Int.MaxValue, onSlowLog: => Unit = {})(block: => Future[R])(implicit ec: ExecutionContext): Future[R] = {
    val t0 = System.nanoTime()
    block.map { result =>
      val t1 = System.nanoTime()
      logTime(taskName, t0, t1, additionalMarkers, slowLogThresholdMs, onSlowLog)
      result
    }
  }

  private def logTime(taskName: String, fromInNs: Long, toInNs: Long, additionalMarkers: LogstashMarker, slowLogThresholdMs: Int, onSlowLog: => Unit) = {
    val durationInMs = (toInNs - fromInNs) / 1000000
    val markers = Markers.appendEntries((
      Map(
        "taskName" -> taskName,
        "durationInMs" -> durationInMs,
        "slowLogThresholdMs" -> slowLogThresholdMs
      )
    ).asJava)
    markers.add(additionalMarkers)

    val message = s"Task $taskName complete in ${durationInMs}ms"

    if (durationInMs > slowLogThresholdMs) {
      logger.warn(s"$message, exceeding slow log threshold of ${slowLogThresholdMs}")(markers)
      println("call onSlowlog")
      onSlowLog
    } else {
      logger.info(message)(markers)
    }
  }
}
