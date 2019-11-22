package services

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import model.{TextBlock, Category, Check, ResponseRule, RuleMatch, ValidatorResponse}
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar._
import utils.Validator

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Promise}

/**
  * A mock validator to test the pool implementation. Doesn't
  * complete work until markAsComplete is called, to test
  * queue behaviour.
  */
class MockValidator(id: Int) extends Validator {
  private var currentWork: Option[Promise[List[RuleMatch]]] = None
  private var currentResponses:  Option[List[RuleMatch]] = None
  def getId = s"mock-validator-$id"
  def getCategory = s"mock-category-$id"
  def getRules = List.empty

  def check(request: ValidatorRequest) = {
    val promise = Promise[List[RuleMatch]]
    val future = promise.future
    currentWork = Some(promise)
    maybeComplete()
    future
  }

  def markAsComplete(responses: List[RuleMatch]): Unit = {
    currentResponses = Some(responses)
    maybeComplete()
  }

  def maybeComplete(): Unit = {
    for {
      response <- currentResponses
      promise <- currentWork
    } yield {
      if (!promise.isCompleted) {
        promise.success(response)
      }
    }
  }

  def fail(message: String) = {
    for {
      promise <- currentWork
    } yield {
      promise.failure(new Throwable(message))
    }
  }
}

class ValidatorPoolTest extends AsyncFlatSpec with Matchers {
  def timeLimit = 1 second
  private implicit val ec = ExecutionContext.global
  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()

  private val responseRule = ResponseRule(
    id = "test-rule",
    description = "test-description",
    category = getCategory(0),
    url = "test-url"
  )

  private val responses = getResponses(List((0, 5, "test-response")))

  private def getValidators(count: Int): List[MockValidator] = {
    (0 until count).map { id =>
      new MockValidator(id)
    }.toList
  }

  private def getCategory(id: Int) = Category(s"mock-category-$id", "Mock category", "Puce")

  private def getPool(validators: List[Validator], maxCurrentJobs: Int = 4, maxQueuedJobs: Int = 100): ValidatorPool = {
    val pool = new ValidatorPool(maxCurrentJobs, maxQueuedJobs)
    validators.zipWithIndex.foreach {
      case (validator, index) => pool.addValidator(getCategory(index), validator)
    }
    pool
  }

  private def getResponses(ruleSpec: List[(Int, Int, String)]) = {
    ruleSpec.map {
      case (from, to, message) =>
        RuleMatch(
          rule = responseRule,
          fromPos = from,
          toPos = to,
          message = message
        )
    }
  }

  private val setId = "set-id"
  private val blockId = "block-id"

  private def getCheck(text: String, categoryIds: Option[List[String]] = None) = Check(
    setId,
    categoryIds,
    List(TextBlock(blockId, text, 0, text.length)))

  "getCurrentCategories" should "report current categories" in {
    val validators = getValidators(1)
    val pool = getPool(validators)
    pool.getCurrentCategories should be(List(("mock-validator-0", getCategory(0))))
  }

  "check" should "return a list of ValidatorResponses" in {
    val validators = getValidators(1)
    val pool = getPool(validators)
    val futureResult = pool.check(getCheck(text = "Example text"))
    validators.head.markAsComplete(responses)
    futureResult.map { result =>
      result shouldBe responses
    }
  }

  "check" should "complete queued jobs" in {
    val validators = getValidators(24)
    val pool = getPool(validators)
    val futureResult = pool.check(getCheck(text = "Example text"))
    validators.foreach(_.markAsComplete(responses))
    ScalaFutures.whenReady(futureResult) { result =>
      result.length shouldBe 24
    }
  }

  "check" should "reject work that exceeds its buffer size" in {
    val validators = getValidators(1)
    // This check should produce a job for each block, filling the queue.
    val checkWithManyBlocks = Check(
      setId,
      None,
      (0 to 100).toList.map { id => TextBlock(id.toString, "Example text", 0, 12) });
    val pool = getPool(validators, 1, 1)
    val futureResult = pool.check(checkWithManyBlocks)
    validators.foreach(_.markAsComplete(responses))
    ScalaFutures.whenReady(futureResult.failed) { e =>
      e.getMessage should include ("full")
    }
  }

  "check" should "handle validation failures" in {
    val validators = getValidators(2)
    val pool = getPool(validators)
    val futureResult = pool.check(getCheck(text = "Example text"))
    val errorMessage = "Something went wrong"
    validators(0).markAsComplete(responses)
    validators(1).fail(errorMessage)
    ScalaFutures.whenReady(futureResult.failed) { e =>
      e.getMessage shouldBe errorMessage
    }
  }

  "check" should "handle requests for categories that do not exist" in {
    val validators = getValidators(2)
    val pool = getPool(validators)
    val futureResult = pool.check(getCheck("Example text", Some(List("category-id-does-not-exist"))))
    ScalaFutures.whenReady(futureResult.failed) { e =>
      e.getMessage should include("unknown category")
    }
  }

  "check" should "emit events when validations are complete" in {
    val validators = getValidators(2)
    val pool = getPool(validators)
    var events = ListBuffer.empty[ValidatorPoolEvent]
    val subscriber = ValidatorPoolSubscriber("set-id", (e: ValidatorPoolEvent) => {
      events += e
      ()
    })
    pool.subscribe(subscriber)
    val check = getCheck("Example text")
    val futureResult = pool.check(check)
    validators.foreach(_.markAsComplete(responses))
    ScalaFutures.whenReady(futureResult) { _ =>
    val categories = validators.map {_.getCategory}
      events.toList shouldBe List(
        ValidatorPoolResultEvent(setId, ValidatorResponse(check.blocks, List(categories(0)), responses)),
        ValidatorPoolResultEvent(setId, ValidatorResponse(check.blocks, List(categories(1)), responses)),
        ValidatorPoolJobsCompleteEvent(setId)
      )
    }
  }
}
