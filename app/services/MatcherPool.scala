package services

import java.util.concurrent.ConcurrentHashMap

import net.logstash.logback.marker.Markers
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}
import play.api.Logger
import model.{BaseRule, Category, Check, MatcherResponse, RuleMatch, TextBlock}
import utils.Matcher
import akka.stream.QueueOfferResult.{Dropped, QueueClosed, Failure => QueueFailure}
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}

case class MatcherRequest(blocks: List[TextBlock], categoryId: String)

/**
  * A PartialMatcherJob represents the information our CheckStrategy needs to divide validation work into jobs.
  */
case class PartialMatcherJob(blocks: List[TextBlock], categoryIds: List[String])

/**
  * A MatcherJob represents everything we need to for a matcher to
  *  - perform work
  *  - notify its caller once work is complete
  *
  */
case class MatcherJob(requestId: String, documentId: String, blocks: List[TextBlock], categoryIds: List[String], promise: Promise[List[RuleMatch]], jobsInValidationSet: Integer) {
  def toMarker = Markers.appendEntries(Map(
    "requestId" -> this.requestId,
    "documentId" -> this.documentId,
    "blocks" -> this.blocks.map(_.id).mkString(", "),
    "categoryIds" -> this.categoryIds.mkString(", ")
  ).asJava)
}

object MatcherPool {
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

class MatcherPool(val maxCurrentJobs: Int = 8, val maxQueuedJobs: Int = 1000, val checkStrategy: MatcherPool.CheckStrategy = MatcherPool.documentPerCategoryCheckStrategy)(implicit ec: ExecutionContext, implicit val mat: Materializer) {
  type JobProgressMap = Map[String, Int]

  private val matchers = new ConcurrentHashMap[String, (Category, Matcher)]().asScala
  private val eventBus = new MatcherPoolEventBus()
  private val queue = Source.queue[MatcherJob](maxQueuedJobs, OverflowStrategy.dropNew)
    .mapAsyncUnordered(maxCurrentJobs)(runValidationJob)
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
      case None => getCurrentCategories.map { case (_, category) => category.id }
      case Some(ids) => ids
    }

    Logger.info(s"Matcher pool query received")(query.toMarker)

    val jobs = createJobsFromPartialJobs(query.requestId, query.documentId.getOrElse("no-document-id"),  checkStrategy(query.blocks, categoryIds))
    val eventuallyResponses = jobs.map(offerJobToQueue)

    Future.sequence(eventuallyResponses).map { matches =>
      Logger.info(s"Matcher pool query complete")(query.toMarker)
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
    * Add a matcher to the pool of matchers for the given category.
    * Replaces a matcher that's already present for that category, returning
    * the replaced matcher.
    */
  def addMatcher(category: Category, matcher: Matcher): Option[(Category, Matcher)] = {
    Logger.info(s"New instance of matcher available of id: ${matcher.getId} for category: ${category.id}")
    matchers.put(category.id, (category, matcher))
  }

  def getCurrentCategories: List[(String, Category)] = {
    val matchersAndCategories = matchers.values.map {
      case (category, matcher) => (matcher.getId, category)
    }.toList
    matchersAndCategories
  }

  def getCurrentRules: List[BaseRule] = {
    matchers.values.flatMap {
      case (_, matcher) =>  matcher.getRules
    }.toList
  }

  private def createJobsFromPartialJobs(requestId: String, documentId: String, partialJobs: List[PartialMatcherJob]) = partialJobs.map { partialJob =>
    val promise = Promise[List[RuleMatch]]
    MatcherJob(requestId, documentId, partialJob.blocks, partialJob.categoryIds, promise, partialJobs.length)
  }

  private def offerJobToQueue(job: MatcherJob): Future[List[RuleMatch]] = {
    Logger.info(s"Job has been offered to the queue")(job.toMarker)

    queue.offer(job).collect {
      case Dropped =>
        failJobWith(job, "Job was dropped from the queue, as the queue is full")
      case QueueClosed =>
        failJobWith(job, s"Job failed because the queue is closed")
      case QueueFailure(err) =>
        failJobWith(job, s"Job failed, reason: ${err.getMessage}")
    }

    job.promise.future.andThen {
      case result => {
        Logger.info(s"Job is complete")(job.toMarker)
        result
      }
    }
  }

  private def failJobWith(job: MatcherJob, message: String) = {
    Logger.error(message)(job.toMarker)
    job.promise.failure(new Throwable(message))
  }

  private def runValidationJob(job: MatcherJob): Future[(MatcherJob, List[RuleMatch])] = {
    val jobResults = job.categoryIds.map { categoryId =>
      matchers.get(categoryId) match {
        case Some((_, matcher)) =>
          val eventuallyMatches = matcher.check(MatcherRequest(job.blocks, categoryId))
          job.promise.completeWith(eventuallyMatches)
          eventuallyMatches
        case None =>
          val message = s"Could not run job with -- unknown category for id: $categoryId"
          Logger.error(message)(job.toMarker)
          val error = new IllegalStateException(message)
          job.promise.failure(error)
          Future.failed(error)
      }
    }

    Future.sequence(jobResults).map { results =>
      (job, results.flatten)
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

