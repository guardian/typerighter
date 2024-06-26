package services

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.gu.typerighter.model.{
  Category,
  CheckSingleRule,
  CheckSingleRuleResult,
  CheckerRule,
  ComparableRegex,
  DictionaryRule,
  Document,
  RegexRule,
  RuleMatch,
  TextBlock,
  TextRange
}
import model._
import org.scalatest.time.SpanSugar._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffShouldMatcher._
import com.softwaremill.diffx.generic.auto._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future, Promise}
import scala.util.Failure
import scala.util.Success
import play.api.libs.concurrent.DefaultFutures
import utils.Matcher

import scala.concurrent.duration.FiniteDuration

/** A mock matcher to test the pool implementation. Doesn't complete work until a response is
  * provided, to test queue behaviour.
  */
class MockMatcher(id: Int) extends Matcher {
  private var currentWork: Option[Promise[List[RuleMatch]]] = None
  private var maybeResponse: Option[Either[String, List[RuleMatch]]] = None
  var maybeRequest: Option[MatcherRequest] = None

  def getType() = s"mock-matcher-$id"
  def getCategories() = Set(Category(s"mock-category-$id", "Mock category"))
  def getRules(): List[CheckerRule] = List.empty

  def check(request: MatcherRequest)(implicit ec: ExecutionContext) = {
    val promise = Promise[List[RuleMatch]]()
    val future = promise.future
    currentWork = Some(promise)
    maybeRequest = Some(request)
    maybeComplete()
    future
  }

  /** When `check` is called, respond with the provided matches.
    */
  def completeWith(responses: List[RuleMatch]): Unit = {
    maybeResponse = Some(Right(responses))
  }

  def failWith(message: String) = {
    maybeResponse = Some(Left(message))
  }

  def maybeComplete(): Unit = {
    for {
      response <- maybeResponse
      promise <- currentWork
    } yield response match {
      case Right(responses) => promise.success(responses)
      case Left(message)    => promise.failure(new Throwable(message))
    }
  }
}

class MockMatcherThatThrows(e: Throwable) extends Matcher {
  def getType() = s"mock-matcher-that-throws"
  def getCategories() = Set(Category(s"mock-category-that-throws", "Mock category"))
  def getRules() = List.empty

  def check(request: MatcherRequest)(implicit ec: ExecutionContext) = {
    Future {
      throw e
    }
  }
}

class MockMatcherWithRules(id: Int, ruleCount: Int) extends MockMatcher(id) {
  override def getRules() = (1 to ruleCount).map { ruleId =>
    DictionaryRule(ruleId.toString, s"word-${ruleId}", Category("category", "category"))
  }.toList
}

class MatcherPoolTest extends AsyncFlatSpec with Matchers {
  def timeLimit() = 1 second
  private implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  private implicit val system: ActorSystem = ActorSystem()

  private def getResponseRule(id: Int) = RegexRule(
    id = "test-rule",
    description = "test-description",
    category = getCategory(id),
    regex = new ComparableRegex("test")
  )

  private val responses = getResponses(List((0, 5, "test-response")))

  private def getMatchers(count: Int): List[MockMatcher] = {
    (0 until count).map { id =>
      new MockMatcher(id)
    }.toList
  }

  private def getCategory(id: Int) = Category(s"mock-category-$id", "Mock category")

  private def getPool(
      matchers: List[Matcher],
      maxCurrentJobs: Int = 4,
      maxQueuedJobs: Int = 100,
      strategy: MatcherPool.CheckStrategy = MatcherPool.documentPerCategoryCheckStrategy,
      checkTimeoutDuration: FiniteDuration = 500 milliseconds
  ): MatcherPool = {
    val futures = new DefaultFutures(system)
    val pool = new MatcherPool(
      maxCurrentJobs,
      maxQueuedJobs,
      strategy,
      futures,
      checkTimeoutDuration = checkTimeoutDuration
    )
    matchers.zipWithIndex.foreach { case (matcher, index) =>
      pool.addMatcher(matcher)
    }
    pool
  }

  private def getResponses(
      ruleSpec: List[(Int, Int, String)],
      categoryId: Int = 0,
      priority: Int = 0
  ): List[RuleMatch] = {
    ruleSpec.map { case (from, to, message) =>
      RuleMatch(
        rule = getResponseRule(categoryId),
        fromPos = from,
        toPos = to,
        precedingText = "",
        subsequentText = "",
        matchedText = "placeholder text",
        message = message,
        matchContext = "[placeholder text]",
        priority = priority
      )
    }
  }

  private val setId = "set-id"
  private val blockId = "block-id"

  private def getCheck(
      text: String,
      categoryIds: Option[Set[String]] = None,
      skippedRanges: List[TextRange] = Nil,
      excludeCategoryIds: Option[Set[String]] = None
  ) = Check(
    Some("example-document"),
    setId,
    categoryIds,
    List(TextBlock(blockId, text, 0, text.length, Some(skippedRanges))),
    excludeCategoryIds
  )

  val checkWithManyBlocks = Check(
    Some("example-document"),
    setId,
    None,
    (0 to 100).toList.map { id => TextBlock(id.toString, "Example text", 0, 12) }
  );

  behavior of "getCurrentCategories"

  it should "report current categories" in {
    val matchers = getMatchers(1)
    val pool = getPool(matchers)
    pool.getCurrentCategories should be(Set(getCategory(0)))
  }

  behavior of "removeMatcherById"

  it should "remove a matcher by its matcher id" in {
    val matchers = getMatchers(2)
    val pool = getPool(matchers)
    pool.removeMatcherById(matchers(1).getId())
    pool.getCurrentCategories should be(Set(getCategory(0)))
  }

  it should "remove all matchers" in {
    val matchers = getMatchers(2)
    val pool = getPool(matchers)
    pool.removeAllMatchers()
    pool.getCurrentCategories should be(Set.empty)
  }

  behavior of "getCurrentRuleCount"

  it should "return the number of rules across all matches" in {
    val matchers = getPool(
      List(
        new MockMatcherWithRules(1, 2),
        new MockMatcherWithRules(1, 3)
      )
    )

    matchers.getCurrentRuleCount shouldBe 5
  }

  behavior of "check"

  it should "return a list of matches and categoryIds checked" in {
    val matchers = getMatchers(1)
    matchers.head.completeWith(responses)

    val pool = getPool(matchers)
    val futureResult = pool.check(getCheck(text = "Example text"))
    futureResult.map { result =>
      result.matches shouldBe responses
      result.categoryIds shouldMatchTo (Set(getCategory(0).id))
    }
  }

  it should "complete queued jobs" in {
    val matchers = getMatchers(24)
    matchers.foreach { _.completeWith(responses) }

    val pool = getPool(matchers)
    val futureResult = pool.check(getCheck(text = "Example text"))

    futureResult.map { result =>
      result.matches.length shouldBe 24
    }
  }

  it should "reject work that exceeds its buffer size" in {
    val matchers = getMatchers(1)
    matchers.foreach { _.completeWith(responses) }

    // This check should produce a job for each block, filling the queue.
    val pool = getPool(matchers, 1, 1, MatcherPool.blockLevelCheckStrategy)
    val futureResult = pool.check(checkWithManyBlocks)
    futureResult transformWith {
      case Success(_) => fail()
      case Failure(e) => e.getMessage should include("full")
    }
  }

  it should "handle validation failures" in {
    val errorMessage = "Something went wrong"
    val matchers = getMatchers(2)
    matchers(0).completeWith(responses)
    matchers(1).failWith(errorMessage)

    val pool = getPool(matchers)
    val futureResult = pool.check(getCheck(text = "Example text"))

    futureResult transformWith {
      case Success(_) => fail()
      case Failure(e) => e.getMessage shouldBe errorMessage
    }
  }

  it should "handle validation failures when the matcher throws an error" in {
    val errorMessage = "Something bad happened"
    val matcher = new MockMatcherThatThrows(new Exception(errorMessage))
    val pool = getPool(List(matcher))
    val futureResult = pool.check(getCheck(text = "Example text"))
    futureResult transformWith {
      case Success(_) => fail()
      case Failure(e) => e.getMessage shouldBe errorMessage
    }
  }

  it should "recover from validation failures" in {
    val errorMessage = "Something went wrong"
    val matchers = getMatchers(1)
    matchers.head.failWith(errorMessage)

    val pool = getPool(matchers)
    val futureResult = pool.check(getCheck(text = "Example text"))

    // Run an initial check, that fails
    val eventualResult = futureResult transformWith {
      case Success(_) => fail()
      case Failure(e) => e.getMessage shouldBe errorMessage
    }

    // The next check should work fine
    eventualResult.flatMap { _ =>
      val anotherResult = pool.check(getCheck(text = "Example text"))
      matchers.head.completeWith(responses)
      anotherResult
    } transformWith {
      case Success(result) => result shouldBe responses
      case Failure(e)      => fail(e)
    }

    eventualResult
  }

  it should "correctly check multiple categories for a single job, and report them" in {
    val matchers = getMatchers(2)
    val firstMatch = getResponses(List((0, 5, "test-response")), 0)
    val secondMatch = getResponses(List((6, 10, "test-response")), 1)
    matchers(0).completeWith(firstMatch)
    matchers(1).completeWith(secondMatch)

    val pool = getPool(matchers)
    val futureResult = pool.check(getCheck(text = "Example text"))
    val expectedCategories = Set(getCategory(0).id, getCategory(1).id)

    futureResult.map { result =>
      result.matches.contains(firstMatch.head) shouldBe true
      result.matches.contains(secondMatch.head) shouldBe true
      result.categoryIds shouldMatchTo (expectedCategories)
    }
  }

  it should "report the categories that are checked, even if no matches are found" in {
    val matchers = getMatchers(1)
    matchers(0).completeWith(getResponses(List.empty))

    val pool = getPool(matchers)
    val futureResult = pool.check(getCheck(text = "Example text"))
    val expectedCategories = Set(getCategory(0).id)

    futureResult.map { result =>
      result.categoryIds shouldBe expectedCategories
    }
  }

  it should "not check categories in excludeCategoryIds" in {
    val matchers = getMatchers(2)
    val categoriesToExclude = matchers.head.getCategories().map(_.id)
    val pool = getPool(matchers, 4, 100, MatcherPool.blockLevelCheckStrategy)
    val firstMatch = getResponses(List((0, 5, "test-response")), 0)
    val secondMatch = getResponses(List((6, 10, "test-response-2")), 1)
    matchers(0).completeWith(firstMatch)
    matchers(1).completeWith(secondMatch)

    val futureResult =
      pool.check(getCheck(text = "Example text", excludeCategoryIds = Some(categoriesToExclude)))

    futureResult.map { result =>
      result.matches.size shouldMatchTo (1)
      result.matches shouldMatchTo (secondMatch)
    }
  }

  it should "handle requests for categories that do not exist" in {
    val matchers = getMatchers(2)
    val pool = getPool(matchers)
    val futureResult = pool.check(getCheck("Example text", Some(Set("category-id-does-not-exist"))))
    futureResult.transformWith {
      case Success(_) => fail()
      case Failure(e) => e.getMessage should include("category-id-does-not-exist")
    }
  }

  it should "time out jobs if they take too long" in {
    val matchers = getMatchers(1)
    val pool = getPool(matchers, checkTimeoutDuration = 500 milliseconds)
    val futureResult = pool.check(getCheck(text = "Example text"))
    futureResult.transformWith {
      case Success(_) => fail()
      case Failure(e) => {
        e.getMessage should include("Timeout")
        e.getMessage should include("500 milliseconds")
      }
    }
  }

  it should "pass text to matchers without the skipped ranges" in {
    val matchers = getMatchers(1)
    val matcher = matchers.head
    matcher.completeWith(responses)

    val pool = getPool(matchers)
    val skippedRanges = List(TextRange(0, 0), TextRange(2, 2), TextRange(4, 4))
    val check = getCheck(
      text = "ABCDEF",
      // We skip A, C and E, so the matcher just sees BDF
      skippedRanges = skippedRanges
    )
    val futureResult = pool.check(check)

    futureResult.map { _ =>
      val expectedBlock = check.blocks.head.copy(
        text = "BDF",
        from = 0,
        to = 3,
        skipRanges = None
      )
      matcher.maybeRequest.get.blocks shouldBe List(expectedBlock)
    }
  }

  it should "map matches that succeed skipped ranges through those ranges to ensure they're correct" in {
    val matchers = getMatchers(1)
    val matcher = matchers.head
    val text = "ABCDEF"
    val skippedRanges = List(TextRange(0, 0), TextRange(2, 2), TextRange(4, 4))
    // The matcher sees "BDF"
    val responses = getResponses(List((0, 0, "This matches B"), (2, 2, "This matches F")))
    matcher.completeWith(responses)

    val pool = getPool(matchers)
    val check = getCheck(
      text = text,
      // We skip the text marked [noted], so the matcher just sees 'Example text with other text'
      skippedRanges = skippedRanges
    )
    val futureResult = pool.check(check)

    futureResult.map { result =>
      val matchRanges = result.matches.map { matches => (matches.fromPos, matches.toPos) }
      matchRanges shouldBe List((1, 1), (5, 5))
    }
  }

  behavior of "checkStream"

  it should "complete queued jobs" in {
    val matchers = getMatchers(24)
    matchers.foreach { _.completeWith(responses) }

    val pool = getPool(matchers)
    val futureResult = pool.checkStream(getCheck(text = "Example text")).runWith(Sink.seq)

    futureResult.map { result =>
      result.length shouldBe 24
    }
  }

  it should "reject work that exceeds its buffer size" in {
    val matchers = getMatchers(1)
    matchers.foreach { _.completeWith(responses) }

    // This check should produce a job for each block, filling the queue.
    val pool = getPool(matchers, 1, 1, MatcherPool.blockLevelCheckStrategy)
    val futureResult = pool.checkStream(checkWithManyBlocks).runWith(Sink.seq)

    futureResult transformWith {
      case Success(result) =>
        println(result)
        fail()
      case Failure(e) => e.getMessage should include("full")
    }
  }

  it should "handle validation failures" in {
    val errorMessage = "Something went wrong"
    val matchers = getMatchers(2)
    matchers(0).completeWith(responses)
    matchers(1).failWith(errorMessage)

    val pool = getPool(matchers)
    val futureResult = pool.checkStream(getCheck(text = "Example text")).runWith(Sink.seq)

    futureResult transformWith {
      case Success(_) => fail()
      case Failure(e) => e.getMessage shouldBe errorMessage
    }
  }

  it should "report on the percentage of completed jobs in each result" in {
    val matchers = getMatchers(10)
    matchers.foreach(_.completeWith(responses))

    val pool = getPool(matchers)
    val futureResult: Future[Seq[CheckResult]] =
      pool.checkStream(getCheck(text = "Example text")).runWith(Sink.seq)

    futureResult map { result =>
      val percentages = result.flatMap(_.percentageRequestComplete)
      percentages shouldBe Seq(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0)
    }
  }

  behavior of "checkSingle"

  it should "complete queued jobs" in {
    val matcher :: Nil = getMatchers(1)
    val rule = getResponseRule(0)
    matcher.completeWith(responses)

    val pool = getPool(List.empty)
    val futureResult = pool
      .checkSingle(
        CheckSingleRule(
          "requestId",
          rule,
          documents = List(
            Document(
              "document-1",
              List(
                TextBlock("blockId", "Example text", 0, 10)
              )
            )
          )
        ),
        matcher
      )
      .runWith(Sink.seq)

    futureResult.map { result =>
      result shouldMatchTo List(
        CheckSingleRuleResult(
          List(
            RuleMatch(
              rule = rule,
              fromPos = 0,
              toPos = 5,
              precedingText = "",
              subsequentText = "",
              matchedText = "placeholder text",
              message = "test-response",
              shortMessage = None,
              suggestions = List(),
              replacement = None,
              matchContext = "[placeholder text]"
            )
          ),
          Some(100)
        )
      )
    }
  }

  it should "give an event for each document" in {
    val matcher :: Nil = getMatchers(1)
    val rule = getResponseRule(0)
    matcher.completeWith(responses)

    val pool = getPool(List.empty)
    val block = TextBlock(
      "blockId",
      "Example text",
      0,
      10
    )
    val futureResult = pool
      .checkSingle(
        CheckSingleRule(
          "requestId",
          rule,
          documents = List(
            Document("document-1", List(block, block)),
            Document("document-2", List(block, block))
          )
        ),
        matcher
      )
      .runWith(Sink.seq)

    futureResult.map { result =>
      result.size shouldBe 2
      result(0).percentageRequestComplete shouldBe Some(50)
      result(1).percentageRequestComplete shouldBe Some(100)
    }
  }

  behavior of "removeOverlappingMatches"

  it should "remove the RuleMatch of lower priority when two RuleMatches overlap" in {
    val matchers = getMatchers(1)
    val pool = getPool(matchers)
    val expectedToBeIgnored = getResponses(List((0, 5, "test-response")), 1, 0)
    val expectedResult = getResponses(List((1, 6, "test-response-2")), 1, 1)
    val overlappingResponses = List(
      expectedToBeIgnored,
      expectedResult
    ).flatten

    val result = pool.removeOverlappingMatches(overlappingResponses)

    result shouldBe expectedResult
  }

  it should "not remove a RuleMatch when two RuleMatches cover distinct ranges" in {
    val matchers = getMatchers(1)
    val pool = getPool(matchers)
    val ruleMatchOne = getResponses(List((0, 5, "test-response")), 1, 0)
    val ruleMatchTwo = getResponses(List((6, 10, "test-response-2")), 1, 1)
    val distinctRangeResponses = List(
      ruleMatchOne,
      ruleMatchTwo
    ).flatten

    val result = pool.removeOverlappingMatches(distinctRangeResponses)

    result shouldBe distinctRangeResponses
  }
}
