package services

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import model._
import org.scalatest._
import org.scalatest.time.SpanSugar._
import com.softwaremill.diffx.scalatest.DiffMatcher._
import utils.Matcher

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Promise}
import scala.util.Failure
import scala.util.Success

/**
  * A mock matcher to test the pool implementation. Doesn't
  * complete work until markAsComplete is called, to test
  * queue behaviour.
  */
class MockMatcher(id: Int) extends Matcher {
  private var currentWork: Option[Promise[List[RuleMatch]]] = None
  private var currentResponses:  Option[List[RuleMatch]] = None
  def getId = s"mock-matcher-$id"
  def getCategory = s"mock-category-$id"
  def getRules = List.empty

  def check(request: MatcherRequest) = {
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

class MatcherPoolTest extends AsyncFlatSpec with Matchers {
  def timeLimit = 1 second
  private implicit val ec = ExecutionContext.global
  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()

  private val responseRule = RegexRule(
    id = "test-rule",
    description = "test-description",
    category = getCategory(0),
    regex = "test"r
  )

  private val responses = getResponses(List((0, 5, "test-response")))

  private def getMatchers(count: Int): List[MockMatcher] = {
    (0 until count).map { id =>
      new MockMatcher(id)
    }.toList
  }

  private def getCategory(id: Int) = Category(s"mock-category-$id", "Mock category", "Puce")

  private def getPool(matchers: List[Matcher], maxCurrentJobs: Int = 4, maxQueuedJobs: Int = 100, strategy: MatcherPool.CheckStrategy = MatcherPool.documentPerCategoryCheckStrategy): MatcherPool = {
    val pool = new MatcherPool(maxCurrentJobs, maxQueuedJobs, strategy)
    matchers.zipWithIndex.foreach {
      case (matcher, index) => pool.addMatcher(getCategory(index), matcher)
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
          matchedText = "placeholder text",
          message = message,
          matchContext = "[placeholder text]"
        )
    }
  }

  private val setId = "set-id"
  private val blockId = "block-id"

  private def getCheck(text: String, categoryIds: Option[List[String]] = None) = Check(
    Some("example-document"),
    setId,
    categoryIds,
    List(TextBlock(blockId, text, 0, text.length)))

  behavior of "getCurrentCategories"

  it should "report current categories" in {
    val matchers = getMatchers(1)
    val pool = getPool(matchers)
    pool.getCurrentCategories should be(List(("mock-matcher-0", getCategory(0), 0)))
  }

  behavior of "removeMatcherByCategory"

  it should "remove a matcher by its category id" in {
    val matchers = getMatchers(2)
    val pool = getPool(matchers)
    pool.removeMatcherByCategory(matchers(1).getCategory())
    pool.getCurrentCategories should be(List(("mock-matcher-0", getCategory(0), 0)))
  }

  it should "remove all matchers" in {
    val matchers = getMatchers(2)
    val pool = getPool(matchers)
    pool.removeAllMatchers
    pool.getCurrentCategories should be(List.empty)
  }

  behavior of "check"

  it should "return a list of MatcherResponses" in {
    val matchers = getMatchers(1)
    val pool = getPool(matchers)
    val futureResult = pool.check(getCheck(text = "Example text"))
    matchers.head.markAsComplete(responses)
    futureResult.map { result =>
      result shouldBe responses
    }
  }

  it should "complete queued jobs" in {
    val matchers = getMatchers(24)
    val pool = getPool(matchers)
    val futureResult = pool.check(getCheck(text = "Example text"))
    matchers.foreach(_.markAsComplete(responses))
    futureResult.map { result =>
      result.length shouldBe 24
    }
  }

  it should "reject work that exceeds its buffer size" in {
    val matchers = getMatchers(1)
    // This check should produce a job for each block, filling the queue.
    val checkWithManyBlocks = Check(
      Some("example-document"),
      setId,
      None,
      (0 to 100).toList.map { id => TextBlock(id.toString, "Example text", 0, 12) });
    val pool = getPool(matchers, 1, 1, MatcherPool.blockLevelCheckStrategy)
    val futureResult = pool.check(checkWithManyBlocks)
    matchers.foreach(_.markAsComplete(responses))
    futureResult transformWith {
      case Success(_) => fail
      case Failure(e) => e.getMessage should include ("full")
    }
  }

  it should "handle validation failures" in {
    val matchers = getMatchers(2)
    val pool = getPool(matchers)
    val futureResult = pool.check(getCheck(text = "Example text"))
    val errorMessage = "Something went wrong"
    val result = futureResult transformWith {
      case Success(_) => fail
      case Failure(e) => e.getMessage shouldBe errorMessage

    }
    matchers(0).markAsComplete(responses)
    matchers(1).fail(errorMessage)
    result
  }

  it should "recover from validation failures" in {
    val matchers = getMatchers(1)
    val pool = getPool(matchers)
    val futureResult = pool.check(getCheck(text = "Example text"))
    val errorMessage = "Something went wrong"

    // Run an initial check, that fails
    val eventualResult = futureResult transformWith {
      case Success(_) => fail
      case Failure(e) => e.getMessage shouldBe errorMessage
    }

    // The next check should work fine
    eventualResult.flatMap { _ =>
      val anotherResult = pool.check(getCheck(text = "Example text"))
      matchers.head.markAsComplete(responses)
      anotherResult
    } transformWith {
      case Success(result) => result shouldBe responses
      case Failure(e) => fail(e)
    }

    matchers.head.fail(errorMessage)
    eventualResult
  }

  it should "correctly check multiple categories for a single job" in {
    val matchers = getMatchers(2)
    val pool = getPool(matchers)
    val futureResult = pool.check(getCheck(text = "Example text"))
    val firstMatch = getResponses(List((0, 5, "test-response")))
    val secondMatch = getResponses(List((6, 10, "test-response")))
    matchers(0).markAsComplete(firstMatch)
    matchers(1).markAsComplete(secondMatch)
    futureResult.map { result =>
      result should matchTo(secondMatch ++ firstMatch)
    }
  }

  it should "correctly check multiple categories for a single job don't overlap" in {
    val matchers = getMatchers(2)
    val pool = getPool(matchers, 4, 100, MatcherPool.blockLevelCheckStrategy)
    val futureResult = pool.check(getCheck(text = "Example text"))
    val firstMatch = getResponses(List((0, 5, "test-response")))
    val secondMatch = getResponses(List((0, 5, "test-response-2")))
    matchers(0).markAsComplete(firstMatch)
    matchers(1).markAsComplete(secondMatch)
    futureResult.map { result =>
      result.size should matchTo(1)
      result should matchTo(secondMatch)
    }
  }

   it should "handle requests for categories that do not exist" in {
    val matchers = getMatchers(2)
    val pool = getPool(matchers)
    val futureResult = pool.check(getCheck("Example text", Some(List("category-id-does-not-exist"))))
    futureResult.transformWith {
      case Success(_) => fail
      case Failure(e) => e.getMessage should include("category-id-does-not-exist")
    }
  }

  it should "emit events when validations are complete" in {
    val matchers = getMatchers(2)
    val pool = getPool(matchers)
    var events = ListBuffer.empty[MatcherPoolEvent]
    val subscriber = MatcherPoolSubscriber("set-id", (e: MatcherPoolEvent) => {
      events += e
      ()
    })
    pool.subscribe(subscriber)
    val check = getCheck("Example text")
    val futureResult = pool.check(check)
    matchers.foreach(_.markAsComplete(responses))
    ScalaFutures.whenReady(futureResult) { _ =>
      val categories = matchers.map {_.getCategory}
      events.toSet shouldBe Set(
        MatcherPoolResultEvent(setId, MatcherResponse(check.blocks, List(categories(0)), responses)),
        MatcherPoolResultEvent(setId, MatcherResponse(check.blocks, List(categories(1)), responses)),
        MatcherPoolJobsCompleteEvent(setId)
      )
    }
  }
}
