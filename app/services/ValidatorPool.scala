package services

import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise, blocking}
import play.api.Logger
import model.{Category, Rule, RuleMatch}
import utils.Validator
import utils.Validator._

import scala.util.{Failure, Success}

case class ValidatorConfig(rules: List[Rule])
case class ValidatorRequest(category: String, text: String)

class ValidatorPool(implicit ec: ExecutionContext) {
  type TValidateCallback = () => Future[ValidatorResponse]

  case class ValidationJob(id: String, categoryId: String, promise: Promise[ValidatorResponse], validate: TValidateCallback)

  private val maxCurrentJobs = 4
  private val maxQueuedJobs = 100
  private val queuedJobs = new ConcurrentLinkedQueue[ValidationJob]()
  private val currentJobs = new ConcurrentLinkedQueue[ValidationJob]()
  private val validators = new ConcurrentHashMap[String, (Category, Validator)]().asScala

  def getMaxCurrentValidations: Int = maxCurrentJobs
  def getMaxQueuedValidations: Int = maxQueuedJobs
  def getQueuedJobCount: Int = queuedJobs.size
  def getCurrentJobCount: Int = currentJobs.size

  /**
    * Check the text with validators assigned to the given category ids.
    * If no ids are assigned, use all the currently available validators.
    */
  def check(id: String, text: String, maybeCategoryIds: Option[List[String]] = None): Future[List[RuleMatch]] = {
    val categoryIds = maybeCategoryIds match {
      case None => getCurrentCategories.map { case (_, category) => category.id }
      case Some(ids) => ids
    }

    Logger.info(s"Validation job with id: ${id} received. Checking categories: ${categoryIds.mkString(", ")}.")

    val eventualChecks = categoryIds.map { categoryId =>
      checkForCategory(id, text, categoryId)
    }

    Future.sequence(eventualChecks).map {
      _.flatten
    }.map { matches =>
      Logger.info(s"Validation job with id: ${id} complete.")
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

  private def checkForCategory(id: String, text: String, categoryId: String): Future[ValidatorResponse] = {
    val promise = Promise[ValidatorResponse]()
    val future = promise.future
    // Wrap our request in a lambda and add it to the queue, ready to be called.
    val validate: () => Future[ValidatorResponse] = () => validators.get(categoryId) match {
      case Some((_, validator)) =>
        val eventualResult = validator.check(ValidatorRequest(categoryId, text))
        eventualResult.andThen {
          case Success(result) => {
            promise.success(result)}
          case Failure(err) => {
            promise.failure(err)
          }
        }
      case None =>
        Future.failed(new IllegalStateException(s"Could not run validation job with id: $id -- unknown category for id: $categoryId"))
    }

    scheduleValidationJob(ValidationJob(id, categoryId, promise, validate))

    future
  }

  private def completeValidationJob(job: ValidationJob): Unit = {
    if (!currentJobs.remove(job)) {
      Logger.warn(s"Validation job ${getJobMessage(job)} has completed, but was not present in current jobs.")
    }
    if (queuedJobs.size > 0) {
      val newJob = queuedJobs.remove()
      Logger.info(s"Starting new validation job ${getJobMessage(job)} from queue. Remaining items in queue: ${queuedJobs.size}")
      scheduleValidationJob(newJob)
    }
    Logger.info(s"Validation job ${getJobMessage(job)} is done. Current jobs: ${currentJobs.toArray.map{ _.toString }.mkString }")
    Unit
  }

  private def scheduleValidationJob(job: ValidationJob) = {
    if (currentJobs.size < maxCurrentJobs) {
      currentJobs.add(job)
      job.validate().andThen {
        case _ => {
          completeValidationJob(job)
        }
      }
      Logger.info(s"Validation job ${getJobMessage(job)} has been called")
    } else if (queuedJobs.size < maxQueuedJobs) {
      queuedJobs.add(job)
      Logger.info(s"Validation job stack full. Added job ${getJobMessage(job)} to queue, which is now of size: ${queuedJobs.size}")
    } else {
      val errorMessage = s"Validation job queue full. Cannot accept new job ${getJobMessage(job)} as queue is already of size: ${queuedJobs.size}"
      Logger.warn(errorMessage)
      job.promise.failure(new Exception(errorMessage))
    }
  }

  private def getJobMessage(job: ValidationJob) = s"with id: ${job.id} for category: ${job.categoryId}"
}