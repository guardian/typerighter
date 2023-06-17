package services

import akka.NotUsed
import akka.stream.QueueOfferResult.{Dropped, QueueClosed, Failure => QueueFailure}
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import com.gu.typerighter.model.{Category, CheckerRule, RuleMatch, TextBlock}
import model._
import net.logstash.logback.marker.{LogstashMarker, Markers}
import play.api.Logging
import play.api.libs.concurrent.Futures
import utils._

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

case class MatcherRequest(blocks: List[TextBlock])

/** A PartialMatcherJob represents the information our CheckStrategy needs to divide validation work
  * into jobs.
  */
case class PartialMatcherJob(blocks: List[TextBlock], categoryIds: Set[String])

/** A MatcherPoolJob represents everything we need to for a matcher to
  *   - perform work
  *   - notify its caller once work is complete
  */
sealed trait MatcherPoolJob {
  val promise: Promise[List[RuleMatch]] = Promise[List[RuleMatch]]()
  val blocks: List[TextBlock]
  def toMarker: LogstashMarker
}

case class MatcherPoolCategoryJob(
    requestId: String,
    documentId: String,
    blocks: List[TextBlock],
    categoryIds: MatcherPool.CategoryIds
) extends MatcherPoolJob {
  def toMarker = Markers.appendEntries(
    Map(
      "requestId" -> this.requestId,
      "documentId" -> this.documentId,
      "blocks" -> this.blocks.map(_.id).mkString(", "),
      "categoryIds" -> this.categoryIds.mkString(", ")
    ).asJava
  )
}

case class MatcherPoolSingleMatcherJob(
    requestId: String,
    documentId: String,
    blocks: List[TextBlock],
    matcher: Matcher
) extends MatcherPoolJob {
  def toMarker = Markers.appendEntries(
    Map(
      "requestId" -> this.requestId,
      "documentId" -> this.documentId,
      "blocks" -> this.blocks.map(_.id).mkString(", ")
    ).asJava
  )
}

object MatcherPool extends Logging {
  type CategoryIds = Set[String]
  type CheckStrategy = (List[TextBlock], CategoryIds) => List[PartialMatcherJob]

  /** This strategy divides the document into single blocks, and evaluates each block against all of
    * the passed categories.
    */
  def blockLevelCheckStrategy: CheckStrategy = (blocks, categoryIds) => {
    blocks.map(block => PartialMatcherJob(List(block), categoryIds))
  }

  /** This strategy evaluates whole documents at once, and evaluates the document separately against
    * each of the passed categories.
    */
  def documentPerCategoryCheckStrategy: CheckStrategy = (blocks, categoryIds) => {
    categoryIds.map(categoryId => PartialMatcherJob(blocks, Set(categoryId))).toList
  }
}

class MatcherPool(
    val maxCurrentJobs: Int = 8,
    val maxQueuedJobs: Int = 1000,
    val checkStrategy: MatcherPool.CheckStrategy = MatcherPool.blockLevelCheckStrategy,
    val futures: Futures,
    val checkSlowLogDuration: FiniteDuration = 5 seconds,
    val checkTimeoutDuration: FiniteDuration = 30 seconds,
    val maybeCloudWatchClient: Option[CloudWatchClient] = None
)(implicit ec: ExecutionContext, implicit val mat: Materializer)
    extends Logging {
  type MatcherId = String
  private val matchers = new ConcurrentHashMap[MatcherId, Matcher]().asScala

  // This supervision strategy resumes the stream when `mapAsyncUnordered`
  // emits failed futures. Without it, the stream fails when errors occur. See
  // https://doc.akka.io/docs/akka/current/stream/stream-error.html#errors-from-mapasync
  private val supervisionStrategy = ActorAttributes.supervisionStrategy(Supervision.resumingDecider)

  private val queue = Source
    .queue[MatcherPoolJob](maxQueuedJobs)
    .mapAsyncUnordered(maxCurrentJobs)(getMatchesForJob)
    .withAttributes(supervisionStrategy)
    .to(Sink.ignore)
    .run()

  /** Check the text with the matchers assigned to the given category ids. If no ids are assigned,
    * use all the currently available matchers.
    */
  def check(query: Check): Future[CheckResult] = {
    val categoryIds = getApplicableCategories(query)

    logCheckBegin(query.toMarker)

    val partialJobs = checkStrategy(query.blocks, categoryIds)
    val jobs = createJobsFromPartialJobs(
      query.requestId,
      query.documentId,
      partialJobs
    )

    val eventuallyResponses = jobs.map(offerJobToQueue)

    Future.sequence(eventuallyResponses).map { responses =>
      val (catIds, blocks, matches) =
        responses.foldLeft((Set.empty[String], List.empty[TextBlock], List.empty[RuleMatch])) {
          case ((accCatIds, accBlocks, accMatches), (job, matches)) =>
            (accCatIds ++ job.categoryIds, accBlocks ++ job.blocks, accMatches ++ matches)
        }
      logCheckComplete(query.toMarker)

      CheckResult(catIds, blocks, matches)
    }
  }

  def checkStream(query: Check): Source[CheckResult, NotUsed] = {
    val categoryIds = getApplicableCategories(query)

    logCheckBegin(query.toMarker)

    val jobs = createJobsFromPartialJobs(
      query.requestId,
      query.documentId,
      checkStrategy(query.blocks, categoryIds)
    )
    val totalJobCount = jobs.size
    val jobsCompleted = new AtomicInteger(0)

    def percentageRequestComplete: Int =
      Math.round(jobsCompleted.floatValue() / totalJobCount * 100)

    logger.info(s"Created $totalJobCount jobs for request with id: ${query.requestId}")

    val eventualResponses = jobs.map(offerJobToQueue)

    Source(eventualResponses)
      .mapAsyncUnordered(1)(identity)
      .map { case (job, matches) =>
        jobsCompleted.incrementAndGet()
        CheckResult(job.categoryIds, job.blocks, matches, Some(percentageRequestComplete))
      }
      .alsoTo(Sink.onComplete { _ => logCheckComplete(query.toMarker) })
  }

  def checkSingle(
      query: CheckSingleRule,
      matcher: Matcher
  ): Source[CheckSingleRuleResult, NotUsed] = {
    logCheckBegin(query.toMarker)

    val jobs = query.documents.map { document =>
      MatcherPoolSingleMatcherJob(
        query.requestId,
        document.id,
        document.blocks,
        matcher
      )
    }

    val totalJobCount = jobs.size
    val jobsCompleted = new AtomicInteger(0)

    def percentageRequestComplete: Int =
      Math.round(jobsCompleted.floatValue() / totalJobCount * 100)

    logger.info(s"Created $totalJobCount jobs for request with id: ${query.requestId}")

    val eventualResponses = jobs.map(offerJobToQueue)

    Source(eventualResponses)
      .mapAsyncUnordered(1)(identity)
      .map { case (_, matches) =>
        jobsCompleted.incrementAndGet()
        CheckSingleRuleResult(matches, Some(percentageRequestComplete))
      }
      .alsoTo(Sink.onComplete { _ => logCheckComplete(query.toMarker) })
  }

  /** Add a matcher to the pool of matchers.
    */
  def addMatcher(matcher: Matcher): Option[Matcher] = {
    logger.info(s"New instance of matcher available of type: ${matcher
        .getType()} for categories: ${matcher.getCategories().map(_.id)}")
    matchers.put(matcher.getId(), matcher)
  }

  /** Remove a matcher from the pool by its id. Returns the removed matcher.
    */
  def removeMatcherById(matcherId: String): Option[Matcher] = {
    matchers.remove(matcherId)
  }

  def removeAllMatchers(): Unit = {
    matchers.clear()
  }

  def getCurrentMatchers: List[Matcher] =
    matchers.values.toList

  def getCurrentCategories: Set[Category] =
    matchers.values.flatMap { _.getCategories() }.toSet

  def getCurrentRules: List[CheckerRule] = {
    matchers.values.flatMap { matcher => matcher.getRules() }.toList
  }

  private def createJobsFromPartialJobs(
      requestId: String,
      documentId: Option[String],
      partialJobs: List[PartialMatcherJob]
  ) = partialJobs.map { partialJob =>
    MatcherPoolCategoryJob(
      requestId,
      documentId.getOrElse("no-document-id"),
      partialJob.blocks,
      partialJob.categoryIds
    )
  }

  private def offerJobToQueue[Job <: MatcherPoolJob](job: Job): Future[(Job, List[RuleMatch])] = {
    logger.info(s"Job has been offered to the queue")(job.toMarker)

    queue.offer(job) match {
      case Dropped =>
        failJobWith(job, "Job was dropped from the queue, as the queue is full")
      case QueueClosed =>
        failJobWith(job, s"Job failed because the queue is closed")
      case QueueFailure(err) =>
        failJobWith(job, s"Job failed, reason: ${err.getMessage}")
      case _ =>
        logger.info(s"Job successfully queued")(job.toMarker)
    }

    job.promise.future.map { result =>
      logger.info("Job is complete")(job.toMarker)
      (job, result)
    }
  }

  private def failJobWith(job: MatcherPoolJob, message: String) = {
    logger.error(message)(job.toMarker)
    job.promise.failure(new Throwable(message))
  }

  private def getMatchesForJob(job: MatcherPoolJob): Future[(MatcherPoolJob, List[RuleMatch])] = {
    val maybeMatchesForJob = for {
      matchers <- job match {
        case categoryJob: MatcherPoolCategoryJob => getMatchersForJob(categoryJob)
        case singleMatcherJob: MatcherPoolSingleMatcherJob =>
          Success(List(singleMatcherJob.matcher))
      }
    } yield {
      val eventuallyMatches = runMatchersForJob(matchers, job.blocks).map { (job, _) }
      job.promise.completeWith(eventuallyMatches.map { case (_, matches) =>
        matches
      })

      eventuallyMatches
    }

    maybeMatchesForJob.failed.map { exception =>
      logger.error(s"Job failed with error: ${exception.getMessage}")(job.toMarker)
      job.promise.failure(exception)
      exception
    }

    Future.fromTry(maybeMatchesForJob).flatten
  }

  private def getMatchersForJob(job: MatcherPoolCategoryJob): Try[List[Matcher]] = {
    val matchersToCheck = matchers.values.toList
      .filter { doesMatcherServeAllCategories(job.categoryIds, _) }

    val availableCategories = matchersToCheck.flatMap(_.getCategories().map(_.id)).toSet
    val missingCategoryIds = job.categoryIds.diff(availableCategories)

    if (missingCategoryIds.nonEmpty) {
      val message =
        s"Could not run job: no matcher for category for id(s): ${missingCategoryIds.mkString(", ")}"
      val exception = new IllegalStateException(message)
      logger.error(message)(job.toMarker)
      Failure(exception)
    } else {
      Success(matchersToCheck)
    }
  }

  private def doesMatcherServeAllCategories(categoryIds: Set[String], matcher: Matcher) =
    categoryIds.exists { matcher.getCategories().map(_.id).contains }

  private def runMatchersForJob(
      matchers: List[Matcher],
      blocks: List[TextBlock]
  ): Future[List[RuleMatch]] = {
    val eventuallyJobResults = matchers.map { matcher =>
      val blocksWithSkippedRangesRemoved = blocks.map(_.removeSkippedRanges())

      val eventuallyCheck = matcher
        .check(MatcherRequest(blocksWithSkippedRangesRemoved))
        .map { matches => matches.map(_.mapMatchThroughBlocks(blocks)) }

      val taskName = s"MatcherPool.runMatchersForJob"
      val taskMarkers = Markers.appendEntries(
        Map(
          "categories" -> matcher.getCategories().map(_.name).mkString(", ")
        ).asJava
      )
      val onSlowLog = (durationMs: Long) =>
        maybeCloudWatchClient.foreach(
          _.putMetric(Metrics.MatcherPoolJobDurationMs, durationMs.toInt)
        )
      Timer.timeAsync(taskName, taskMarkers, checkSlowLogDuration.toMillis, onSlowLog) {
        futures.timeout(checkTimeoutDuration)(eventuallyCheck)
      }
    }

    val eventuallyAllMatches = Future.sequence(eventuallyJobResults).map { _.flatten }

    eventuallyAllMatches.map(removeOverlappingMatches)
  }

  private def removeOverlappingMatches(matches: List[RuleMatch]) = {
    val matchesByCategory = matches.groupBy(_.rule.category.id).toList

    val sortedMatches = matchesByCategory.sortBy { case (categoryId, matches) =>
      categoryId
    }

    sortedMatches.foldLeft(List.empty[RuleMatch])((acc, currentMatches) =>
      currentMatches match {
        case (_, matches) => RuleMatchHelpers.removeOverlappingRules(acc, matches) ++ matches
      }
    )
  }

  private def logCheckBegin(markers: LogstashMarker) =
    logger.info(s"Matcher pool query begins")(markers)
  private def logCheckComplete(markers: LogstashMarker) =
    logger.info(s"Matcher pool query complete")(markers)

  /** Get the categories to apply to this check. If no categories are supplied, default to all
    * available categories.
    */
  private def getApplicableCategories(query: Check) = query.categoryIds match {
    case None      => getCurrentCategories.map(_.id)
    case Some(ids) => ids
  }
}
