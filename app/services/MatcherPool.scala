package services

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException

import net.logstash.logback.marker.Markers

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Failure
import scala.concurrent.duration._
import scala.util.Success

import model.{BaseRule, Category, Check, MatcherResponse, RuleMatch, TextBlock}
import utils.{Matcher, RuleMatchHelpers}
import akka.stream.QueueOfferResult.{Dropped, QueueClosed, Failure => QueueFailure}
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import play.api.libs.concurrent.Futures
import play.api.Logging

case class MatcherRequest(blocks: List[TextBlock])

/**
  * A PartialMatcherJob represents the information our CheckStrategy needs to divide validation work into jobs.
  */
case class PartialMatcherJob(blocks: List[TextBlock], categoryIds: List[String])

/**
  * A MatcherJob represents everything we need to for a matcher to
  *  - perform work
  *  - notify its caller once work is complete
  */
case class MatcherJob(requestId: String, documentId: String, blocks: List[TextBlock], categoryIds: List[String], promise: Promise[List[RuleMatch]], jobsInValidationSet: Integer) {
  def toMarker = Markers.appendEntries(Map(
    "requestId" -> this.requestId,
    "documentId" -> this.documentId,
    "blocks" -> this.blocks.map(_.id).mkString(", "),
    "categoryIds" -> this.categoryIds.mkString(", ")
  ).asJava)
}

object MatcherPool extends Logging {
  type CategoryIds = List[String]
  type CheckStrategy = (List[TextBlock], CategoryIds) => List[PartialMatcherJob]

  /**
    * This strategy divides the document into single blocks, and evaluates each block
    * against all of the passed categories.
    */
  def blockLevelCheckStrategy: CheckStrategy = (blocks, categoryIds) => {
    blocks.map(block => PartialMatcherJob(List(block), categoryIds))
  }

  /**
    * This strategy evaluates whole documents at once, and evaluates the document
    * separately against each of the passed categories.
    */
  def documentPerCategoryCheckStrategy: CheckStrategy = (blocks, categoryIds) => {
    categoryIds.map(categoryId => PartialMatcherJob(blocks, List(categoryId)))
  }
}

class MatcherPool(
  val maxCurrentJobs: Int = 8,
  val maxQueuedJobs: Int = 1000,
  val checkStrategy: MatcherPool.CheckStrategy = MatcherPool.blockLevelCheckStrategy,
  val futures: Futures,
  val checkTimeoutDuration: FiniteDuration = 5 seconds
)(implicit ec: ExecutionContext, implicit val mat: Materializer) extends Logging {
  type JobProgressMap = Map[String, Int]
  type MatcherId = String
  private val matchers = new ConcurrentHashMap[MatcherId, Matcher]().asScala
  private val eventBus = new MatcherPoolEventBus()

  // This supervision strategy resumes the stream when `mapAsyncUnordered`
  // emits failed futures. Without it, the stream fails when errors occur. See
  // https://doc.akka.io/docs/akka/current/stream/stream-error.html#errors-from-mapasync
  private val supervisionStrategy = ActorAttributes.supervisionStrategy(Supervision.resumingDecider)

  private val queue = Source.queue[MatcherJob](maxQueuedJobs, OverflowStrategy.dropNew)
    .mapAsyncUnordered(maxCurrentJobs)(runValidationJob)
    .withAttributes(supervisionStrategy)
    .to(Sink.fold(Map[String, Int]())(markJobAsComplete))
    .run()

  def getMaxCurrentValidations: Int = maxCurrentJobs
  def getMaxQueuedValidations: Int = maxQueuedJobs

  /**
    * Check the text with the matchers assigned to the given category ids.
    * If no ids are assigned, use all the currently available matchers.
    */
  def check(query: Check): Future[List[RuleMatch]] = {
    val categoryIds = query.categoryIds match {
      case None => getCurrentCategories.map { case (_, category, _) => category.id }
      case Some(ids) => ids
    }

    logger.info(s"Matcher pool query received")(query.toMarker)

    val jobs = createJobsFromPartialJobs(query.requestId, query.documentId.getOrElse("no-document-id"),  checkStrategy(query.blocks, categoryIds))
    val eventuallyResponses = jobs.map(offerJobToQueue)

    Future.sequence(eventuallyResponses).map { matches =>
      logger.info(s"Matcher pool query complete")(query.toMarker)
      matches.flatten
    }
  }

  /**
    * @see MatcherPoolEventBus
    */
  def subscribe(subscriber: MatcherPoolSubscriber): Boolean = eventBus.subscribe(subscriber, subscriber.requestId)

  /**
    * @see MatcherPoolEventBus
    */
  def unsubscribe(subscriber: MatcherPoolSubscriber): Boolean = eventBus.unsubscribe(subscriber, subscriber.requestId)

  /**
    * Add a matcher to the pool of matchers.
    */
  def addMatcher(matcher: Matcher): Option[Matcher] = {
    logger.info(s"New instance of matcher available of type: ${matcher.getType} for category: ${matcher.getCategory().id}")
    matchers.put(matcher.getId(), matcher)
  }

  /**
    * Remove a matcher from the pool by its id.
    * Returns the removed matcher.
    */
  def removeMatcherByCategory(matcherId: String): Option[Matcher] = {
    matchers.remove(matcherId)
  }

  def removeAllMatchers(): Unit = {
    matchers.map(_._1).foreach(removeMatcherByCategory)
  }

  def getCurrentCategories: List[(String, Category, Int)] = {
<<<<<<< HEAD
    val matchersAndCategories = matchers.values.map {
      case (category, matcher) => (matcher.getType, category, matcher.getRules.length)
=======
    val matchersAndCategories = matchers.values.map { matcher =>
      (matcher.getId, matcher.getCategory, matcher.getRules.length)
>>>>>>> 25dd2b9e... Seal BaseRule trait for serialisation purposes; allow multiple matchers per category
    }.toList
    matchersAndCategories
  }

  def getCurrentRules: List[BaseRule] = {
    matchers.values.flatMap { matcher => matcher.getRules }.toList
  }

  private def createJobsFromPartialJobs(requestId: String, documentId: String, partialJobs: List[PartialMatcherJob]) = partialJobs.map { partialJob =>
    val promise = Promise[List[RuleMatch]]
    MatcherJob(requestId, documentId, partialJob.blocks, partialJob.categoryIds, promise, partialJobs.length)
  }

  private def offerJobToQueue(job: MatcherJob): Future[List[RuleMatch]] = {
    logger.info(s"Job has been offered to the queue")(job.toMarker)

    queue.offer(job).collect {
      case Dropped =>
        failJobWith(job, "Job was dropped from the queue, as the queue is full")
      case QueueClosed =>
        failJobWith(job, s"Job failed because the queue is closed")
      case QueueFailure(err) =>
        failJobWith(job, s"Job failed, reason: ${err.getMessage}")
    }

    job.promise.future.map {
      case result => {
        logger.info("Job is complete")(job.toMarker)
        result
      }
    }
  }

  private def failJobWith(job: MatcherJob, message: String) = {
    logger.error(message)(job.toMarker)
    job.promise.failure(new Throwable(message))
  }

  private def runValidationJob(job: MatcherJob): Future[(MatcherJob, List[RuleMatch])] = {
    val matchersAndCategoryIds = job.categoryIds
      .map(categoryId => matchers.values.find(_.getCategory().id == categoryId))
      .zip(job.categoryIds)

    val eventuallyJobResults : List[Future[List[RuleMatch]]] = matchersAndCategoryIds.map {
      case (Some(matcher), _) =>
        val eventuallyCheck = matcher.check(MatcherRequest(job.blocks))
        futures.timeout(checkTimeoutDuration)(eventuallyCheck)
      case (None, categoryId) =>
        val message = s"Could not run job: no matcher for category for id: $categoryId"
        logger.error(message)(job.toMarker)
        val error = new IllegalStateException(message)
        Future.failed(error)
    }

    Future.sequence(eventuallyJobResults).map { matchesByCategory =>
      val sortedMatches = matchesByCategory.sortBy {
        case matches => matches.headOption.map(_.rule.category.id).getOrElse("no-category")
      }.foldLeft(List.empty[RuleMatch])(
        (acc, matches) => {
          RuleMatchHelpers.removeOverlappingRules(acc, matches) ++ matches
        }
      )
      job.promise.completeWith(Future.successful(sortedMatches))
      (job, sortedMatches)
    }.andThen {
      case Failure(exception) => {
        logger.error(s"Job failed with error: ${exception.getMessage}")(job.toMarker)
        job.promise.failure(exception)
      }
    }
  }

  private def markJobAsComplete(progressMap: Map[String, Int], result: (MatcherJob, List[RuleMatch])): JobProgressMap = {
    result match {
      case (job, matches) =>
        val newCount = progressMap.get(job.requestId) match {
          case Some(jobCount) => jobCount - 1
          case None => job.jobsInValidationSet - 1
        }

        publishResults(job, matches)

        if (newCount == 0) {
          publishJobsComplete(job.requestId)
        }

        progressMap + (job.requestId -> newCount)
    }
  }

  private def publishResults(job: MatcherJob, results: List[RuleMatch]): Unit = {
    eventBus.publish(MatcherPoolResultEvent(
      job.requestId,
      MatcherResponse(
        job.blocks,
        job.categoryIds,
        results
      )
    ))
  }

  private def publishJobsComplete(requestId: String): Unit = {
    eventBus.publish(MatcherPoolJobsCompleteEvent(requestId))
  }
}
