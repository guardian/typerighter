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
import model.CheckQuery

case class ValidatorConfig(rules: List[Rule])
case class ValidatorRequest(category: String, text: String, from: Integer, to: Integer)

class ValidatorPool(val maxCurrentJobs: Int = 8, val maxQueuedJobs: Int = 1000)(implicit ec: ExecutionContext, implicit val mat: Materializer) {
  case class ValidationJob(id: String, categoryId: String, text: String, from: Integer, to: Integer, promise: Promise[ValidatorResponse])

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
  def check(query: CheckQuery): Future[List[RuleMatch]] = {
    val categoryIds = query.categoryIds match {
      case None => getCurrentCategories.map { case (_, category) => category.id }
      case Some(ids) => ids
    }

    Logger.info(s"Validation job with id: ${query.validationId} received. Checking categories: ${categoryIds.mkString(", ")}")
    val eventualChecks = categoryIds.map { categoryId =>
      checkForCategory(query, categoryId)
    }

    Future.sequence(eventualChecks).map {
      _.flatten
    }.map { matches =>
      Logger.info(s"Validation job with id: ${query.validationId} complete")
      matches
    }
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

  private def checkForCategory(query: CheckQuery, categoryId: String): Future[ValidatorResponse] = {
    val promise = Promise[ValidatorResponse]()
    val job = ValidationJob(query.validationId, categoryId, query.text, query.from, query.to, promise)

    Logger.info(s"Job ${getJobMessage(job)} has been offered to the queue")

    queue.offer(job).collect {
      case Dropped =>
        promise.failure(new Throwable(s"Job ${getJobMessage(job)} was dropped from the queue, as the queue is full"))
      case QueueClosed =>
        promise.failure(new Throwable(s"Job ${getJobMessage(job)} failed because the queue is closed"))
      case QueueFailure(err) =>
        promise.failure(new Throwable(s"Job ${getJobMessage(job)} failed, reason: ${err.getMessage}"))
    }

    promise.future.andThen {
      case result => {
        Logger.info(s"Job ${getJobMessage(job)} is complete")
        result
      }
    }
  }

  private def runValidationJob(job: ValidationJob): Future[ValidatorResponse] = {
    validators.get(job.categoryId) match {
      case Some((_, validator)) =>
        val eventualResult = validator.check(ValidatorRequest(job.categoryId, job.text, job.from, job.to)).map { results =>     
          // Map the position
          results.map { result => result.copy( fromPos = result.fromPos + job.from, toPos = result.toPos + job.from )}
        }
        job.promise.completeWith(eventualResult)
        eventualResult
      case None =>
        val error = new IllegalStateException(s"Could not run validation job with id: ${job.id} -- unknown category for id: ${job.categoryId}")
        job.promise.failure(error)
        Future.failed(error)
    }
  }

  private def getJobMessage(job: ValidationJob) = s"with id: ${job.id} for category: ${job.categoryId}"
}