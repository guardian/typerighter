package scala.utils

import model._
import org.scalatest.flatspec.AnyFlatSpec
import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito }
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffMatcher._

import utils.Timer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._

class TimerTest extends AnyFlatSpec with Matchers with IdiomaticMockito {
  implicit val executionContext = ExecutionContext.Implicits.global

  behavior of "time"

  it should "not call `onSlowLog` when a task does not its slow log threshold" in {
    val mockFunction = mock[() => Unit]

    Timer.time(taskName = "task", slowLogThresholdMs = 100, onSlowLog = mockFunction.apply()){}

    mockFunction() wasNever called
  }

  it should "call `onSlowLog` when a task exceeds its slow log threshold" in {
    val mockFunction = mock[() => Unit]

    Timer.time(taskName = "task", slowLogThresholdMs = 100, onSlowLog = mockFunction.apply()){
      Thread.sleep(200)
    }

    mockFunction() was called
  }

  behavior of "timeAsync"

  it should "not call `onSlowLog` when a task does not its slow log threshold" in {
    val mockFunction = mock[() => Unit]

    Timer.timeAsync(taskName = "task", slowLogThresholdMs = 100, onSlowLog = mockFunction.apply()){
      Future.successful(())
    }

    mockFunction() wasNever called
  }

  it should "call `onSlowLog` when a task exceeds its slow log threshold" in {
    val mockFunction = mock[() => Unit]

    val task = Timer.timeAsync(taskName = "task", slowLogThresholdMs = 100, onSlowLog = mockFunction.apply()){
      Thread.sleep(200)
      Future.successful(())
    }
    Await.result(task, 1.second)

    mockFunction() was called
  }
}
