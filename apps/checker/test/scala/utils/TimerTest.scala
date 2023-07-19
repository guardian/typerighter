package utils

import org.scalatest.flatspec.AnyFlatSpec
import org.mockito.{IdiomaticMockito}
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._

class TimerTest extends AnyFlatSpec with Matchers with IdiomaticMockito {
  implicit val executionContext = ExecutionContext.Implicits.global

  behavior of "time"

  it should "not call `onSlowLog` when a task does not exceed its slow log threshold" in {
    val mockFunction = spyLambda((d: Long) => ())

    Timer.time(taskName = "task", slowLogThresholdMs = 100, onSlowLog = mockFunction) {}

    mockFunction wasNever called
  }

  it should "call `onSlowLog` when a task exceeds its slow log threshold" in {
    var eventualDuration = 0L
    val mockFunction = spyLambda((duration: Long) => eventualDuration = duration)

    Timer.time(taskName = "task", slowLogThresholdMs = 100, onSlowLog = mockFunction) {
      Thread.sleep(200)
    }

    eventualDuration.toInt should be >= 200
  }

  behavior of "timeAsync"

  it should "not call `onSlowLog` when a task does not exceed its slow log threshold" in {
    val mockFunction = spyLambda((d: Long) => ())

    Timer.timeAsync(taskName = "task", slowLogThresholdMs = 100, onSlowLog = mockFunction) {
      Future.successful(())
    }

    mockFunction wasNever called
  }

  it should "call `onSlowLog` when a task exceeds its slow log threshold" in {
    var eventualDuration = 0L
    val mockFunction = (duration: Long) => eventualDuration = duration

    val task =
      Timer.timeAsync(taskName = "task", slowLogThresholdMs = 100, onSlowLog = mockFunction) {
        Thread.sleep(200)
        Future.successful(())
      }
    Await.result(task, 1.second)

    eventualDuration.toInt should be >= 200
  }
}
