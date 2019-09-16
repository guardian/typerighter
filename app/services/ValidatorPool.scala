package services

import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}
import play.api.Logger
import model.{Category, Rule, RuleMatch}
import utils.Validator
import utils.Validator._

import akka.stream.QueueOfferResult.{Dropped, Failure => QueueFailure, QueueClosed}
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import model.Check
import model.Block

case class ValidatorConfig(rules: List[Rule])
case class ValidatorRequest(blocks: List[Block], categoryId: String)
case class ValidationJob(blocks: List[Block], categoryId: String, promise: Promise[ValidatorResponse])
case class CheckResponse(noOfScheduledJobs: Integer, future: Future[ValidatorResponse])

object ValidatorPool {
  type CategoryIds = List[String]
  type CheckStrategy = (List[Block], CategoryIds) => List[ValidationJob]

  def blockLevelCheckStrategy: CheckStrategy = (queries, categoryIds) => {
    for {
      categoryId <- categoryIds
      query <- queries
    } yield {
      val promise = Promise[ValidatorResponse]()
      ValidationJob(List(query), categoryId, promise)
    }
  }

  def documentLevelCheckStrategy: CheckStrategy = (queries, categoryIds) => {
    for {
      categoryId <- categoryIds
    } yield {
      val promise = Promise[ValidatorResponse]()
      ValidationJob(queries, categoryId, promise)
    }
  }
}

class ValidatorPool(val maxCurrentJobs: Int = 8, val maxQueuedJobs: Int = 1000, val checkStrategy: ValidatorPool.CheckStrategy = ValidatorPool.blockLevelCheckStrategy)(implicit ec: ExecutionContext, implicit val mat: Materializer) {

  private val validators = new ConcurrentHashMap[String, (Category, Validator)]().asScala

  private val queue = Source.queue[ValidationJob](maxQueuedJobs, OverflowStrategy.dropNew)
    .mapAsyncUnordered(maxCurrentJobs)(runValidationJob)
    .to(Sink.ignore)
    .run()

  def getMaxCurrentValidations: Int = maxCurrentJobs
  def getMaxQueuedValidations: Int = maxQueuedJobs

  /**
    * Check the text with the validators assigned to the given category ids.
    * If no ids are assigned, use all the currently available validators.
    */
  def check(query: Check): CheckResponse = {
    val categoryIds = query.categoryIds match {
      case None => getCurrentCategories.map { case (_, category) => category.id }
      case Some(ids) => ids
    }

    Logger.info(s"Validation job with id: ${query.validationSetId} received. Checking categories: ${categoryIds.mkString(", ")}")
    
    val eventuallyResponses = checkStrategy(query.blocks, categoryIds).map(offerJobToQueue)

    val futureOfAllWork = Future.sequence(eventuallyResponses).map {
      _.flatten
    }.map { matches =>
      Logger.info(s"Validation job with id: ${query.validationSetId} complete")
      matches
    }

    CheckResponse(eventuallyResponses.length, futureOfAllWork)
  }

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

  private def offerJobToQueue(job: ValidationJob): Future[ValidatorResponse] = {
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

  private def runValidationJob(job: ValidationJob): Future[ValidatorResponse] = {
    validators.get(job.categoryId) match {
      case Some((_, validator)) =>
        val eventualResult = validator.check(ValidatorRequest(job.blocks, job.categoryId))
        job.promise.completeWith(eventualResult)
        eventualResult
      case None =>
        val error = new IllegalStateException(s"Could not run validation job with blocks: ${getJobBlockIdsAsString(job)} -- unknown category for id: ${job.categoryId}")
        job.promise.failure(error)
        Future.failed(error)
    }
  }

  private def getJobMessage(job: ValidationJob) = s"with blocks: ${getJobBlockIdsAsString(job)} for category: ${job.categoryId}"

  private def getJobBlockIdsAsString(job: ValidationJob) = job.blocks.map(_.id).mkString(", ")
}

