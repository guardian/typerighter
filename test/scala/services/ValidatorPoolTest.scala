package services

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import model.{Category, ResponseRule, RuleMatch}
import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar._
import utils.Validator
import utils.Validator.ValidatorResponse

import scala.concurrent.{ExecutionContext, Promise}

/**
  * A mock validator to test the pool implementation. Doesn't
  * complete work until markAsComplete is called, to test
  * queue behaviour.
  */
class MockValidator(id: Int) extends Validator {
  private var currentWork: Option[Promise[ValidatorResponse]] = None
  private var currentResponses:  Option[ValidatorResponse] = None
  def getId = s"mock-validator-$id"
  def getCategory = "mock-validator-cat"
  def getRules = List.empty

  def check(request: ValidatorRequest) = {
    val promise = Promise[ValidatorResponse]()
    val future = promise.future
    currentWork = Some(promise)
    maybeComplete()
    future
  }

  def markAsComplete(responses: ValidatorResponse): Unit = {
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
      promise.failure(new Exception(message))
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
    (1 to count).map { id =>
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

  "getCurrentCategories" should "report current categories" in {
    val validators = getValidators(1)
    val pool = getPool(validators)
    pool.getCurrentCategories should be(List(("mock-validator-1", getCategory(0))))
  }

  "check" should "return a list of ValidatorResponses" in {
    val validators = getValidators(1)
    val pool = getPool(validators)
    val eventuallyResult = pool.check("test-1", "Example text")
    validators.head.markAsComplete(responses)
    eventuallyResult.map { result =>
      result shouldBe responses
    }
  }

  "check" should "reject work that exceeds its buffer size" in {
    val validators = getValidators(100)
    val pool = getPool(validators, 1, 0)
    val eventuallyFails = pool.check("test-1", "Example text")
    validators.foreach(_.markAsComplete(responses))
    ScalaFutures.whenReady(eventuallyFails.failed) { e =>
      e.getMessage should include ("full")
    }
  }

  "check" should "complete queued jobs" in {
    val validators = getValidators(24)
    val pool = getPool(validators)
    val checkFuture = pool.check("test-1", "Example text")
    validators.foreach(_.markAsComplete(responses))
    ScalaFutures.whenReady(checkFuture) { result =>
      result.length shouldBe 24
    }
  }

  "check" should "handle validation failures" in {
    val validators = getValidators(2)
    val pool = getPool(validators)
    val eventuallyFails = pool.check("test-1", "Example text")
    val errorMessage = "Something went wrong"
    validators(0).markAsComplete(responses)
    validators(1).fail(errorMessage)
    ScalaFutures.whenReady(eventuallyFails.failed) { e =>
      e.getMessage shouldBe errorMessage
    }
  }

  "check" should "handle requests for categories that do not exist" in {
    val validators = getValidators(2)
    val pool = getPool(validators)
    val eventuallyFails = pool.check("test-1", "Example text", Some(List("category-does-not-exist")))
    ScalaFutures.whenReady(eventuallyFails.failed) { e =>
      e.getMessage should include("unknown category")
    }
  }
}
