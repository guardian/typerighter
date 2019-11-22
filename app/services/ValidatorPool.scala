package services

import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}
import play.api.Logger
import model.{TextBlock, Category, Check, Rule, RuleMatch, ValidatorResponse}
import utils.Validator
import akka.stream.QueueOfferResult.{Dropped, QueueClosed, Failure => QueueFailure}
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}

case class ValidatorConfig(rules: List[Rule])
case class ValidatorRequest(blocks: List[TextBlock], categoryId: String)

/**
  * A PartialValidationJob represents the information our CheckStrategy needs to divide validation work into jobs.
  */
case class PartialValidationJob(blocks: List[TextBlock], categoryIds: List[String])

/**
  * A ValidationJob represents everything we need to:
  *  - perform validation work
  *  - notify the job creator when that work is done.
  */
case class ValidationJob(blocks: List[TextBlock], categoryIds: List[String], requestId: String, promise: Promise[List[RuleMatch]], jobsInValidationSet: Integer)

object ValidatorPool {
  type CategoryIds = List[String]
  type CheckStrategy = (List[TextBlock], CategoryIds) => List[PartialValidationJob]


  /**
    * This strategy divides the document into single blocks, and evaluates each block
    * against all of the passed categories.
    */
  def blockLevelCheckStrategy: CheckStrategy = (blocks, categoryIds) => {
    blocks.map(block => PartialValidationJob(List(block), categoryIds))
  }

  /**
    * This strategy evaluates whole documents at once, and evaluates the document
    * separately against each of the passed categories.
    */
  def documentPerCategoryCheckStrategy: CheckStrategy = (blocks, categoryIds) => {
    categoryIds.map(categoryId => PartialValidationJob(blocks, List(categoryId)))
  }
}

class ValidatorPool(val maxCurrentJobs: Int = 8, val maxQueuedJobs: Int = 1000, val checkStrategy: ValidatorPool.CheckStrategy = ValidatorPool.documentPerCategoryCheckStrategy)(implicit ec: ExecutionContext, implicit val mat: Materializer) {
  type JobProgressMap = Map[String, Int]

  private val validators = new ConcurrentHashMap[String, (Category, Validator)]().asScala
  private val eventBus = new ValidatorPoolEventBus()
  private val queue = Source.queue[ValidationJob](maxQueuedJobs, OverflowStrategy.dropNew)
    .mapAsyncUnordered(maxCurrentJobs)(runValidationJob)
    .to(Sink.fold(Map[String, Int]())(markJobAsComplete))
    .run()

  def getMaxCurrentValidations: Int = maxCurrentJobs
  def getMaxQueuedValidations: Int = maxQueuedJobs

  /**
    * Check the text with the validators assigned to the given category ids.
    * If no ids are assigned, use all the currently available validators.
    */
  def check(query: Check): Future[List[RuleMatch]] = {
    val categoryIds = query.categoryIds match {
      case None => getCurrentCategories.map { case (_, category) => category.id }
      case Some(ids) => ids
    }

    Logger.info(s"Validation job with id: ${query.requestId} received. Checking categories: ${categoryIds.mkString(", ")}")
    
    val eventuallyResponses =
      createJobsFromPartialJobs(checkStrategy(query.blocks, categoryIds), query.requestId)
      .map(offerJobToQueue)

    Future.sequence(eventuallyResponses).map { matches =>
      Logger.info(s"Validation job with id: ${query.requestId} complete")
      matches.flatten
    }
  }

  /**
    * @see ValidatorPoolEventBus
    */
  def subscribe(subscriber: ValidatorPoolSubscriber): Boolean = eventBus.subscribe(subscriber, subscriber.requestId)

  /**
    * @see ValidatorPoolEventBus
    */
  def unsubscribe(subscriber: ValidatorPoolSubscriber): Boolean = eventBus.unsubscribe(subscriber, subscriber.requestId)

  /**
    * Add a validator to the pool of validators for the given category.
    * Replaces a validator that's already present for that category, returning
    * the replaced validator.
    */
  def addValidator(category: Category, validator: Validator): Option[(Category, Validator)] = {
    Logger.info(s"New instance of validator available of id: ${validator.getId} for category: ${category.id}")
    validators.put(category.id, (category, validator))
  }

  def getCurrentCategories: List[(String, Category)] = {
    val validatorsAndCategories = validators.values.map {
      case (category, validator) => (validator.getId, category)
    }.toList
    validatorsAndCategories
  }

  def getCurrentRules: List[Rule] = {
    validators.values.flatMap {
      case (_, validator) =>  validator.getRules
    }.toList
  }

  private def createJobsFromPartialJobs(partialJobs: List[PartialValidationJob], requestId: String) = partialJobs.map { partialJob =>
    val promise = Promise[List[RuleMatch]]
    ValidationJob(partialJob.blocks, partialJob.categoryIds, requestId, promise, partialJobs.length)
  }

  private def offerJobToQueue(job: ValidationJob): Future[List[RuleMatch]] = {
    Logger.info(s"Job ${getJobMessage(job)} has been offered to the queue")

    queue.offer(job).collect {
      case Dropped =>
        job.promise.failure(new Throwable(s"Job ${getJobMessage(job)} was dropped from the queue, as the queue is full"))
      case QueueClosed =>
        job.promise.failure(new Throwable(s"Job ${getJobMessage(job)} failed because the queue is closed"))
      case QueueFailure(err) =>
        job.promise.failure(new Throwable(s"Job ${getJobMessage(job)} failed, reason: ${err.getMessage}"))
    }

    job.promise.future.andThen {
      case result => {
        Logger.info(s"Job ${getJobMessage(job)} is complete")
        result
      }
    }
  }

  private def runValidationJob(job: ValidationJob): Future[(ValidationJob, List[RuleMatch])] = {
    val jobResults = job.categoryIds.map { categoryId =>
      validators.get(categoryId) match {
        case Some((_, validator)) =>
          val eventuallyMatches = validator.check(ValidatorRequest(job.blocks, categoryId))
          job.promise.completeWith(eventuallyMatches)
          eventuallyMatches
        case None =>
          val error = new IllegalStateException(s"Could not run validation job with blocks: ${getJobBlockIdsAsString(job)} -- unknown category for id: $categoryId")
          job.promise.failure(error)
          Future.failed(error)
      }
    }

    Future.sequence(jobResults).map { results =>
      (job, results.flatten)
    }
  }

  private def markJobAsComplete(progressMap: Map[String, Int], result: (ValidationJob, List[RuleMatch])): JobProgressMap = {
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

  private def publishResults(job: ValidationJob, results: List[RuleMatch]): Unit = {
    eventBus.publish(ValidatorPoolResultEvent(
      job.requestId,
      ValidatorResponse(
        job.blocks,
        job.categoryIds,
        results
      )
    ))
  }

  private def publishJobsComplete(requestId: String): Unit = {
    eventBus.publish(ValidatorPoolJobsCompleteEvent(requestId))
  }

  private def getJobMessage(job: ValidationJob) = s"with blocks: ${getJobBlockIdsAsString(job)} for categories: ${job.categoryIds.mkString(", ")}"

  private def getJobBlockIdsAsString(job: ValidationJob) = job.blocks.map(_.id).mkString(", ")
}

