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

trait ValidatorFactory {
  def createInstance(name: String, config: ValidatorConfig)(implicit ec: ExecutionContext): (Validator, List[String])
}

class ValidatorPool(implicit ec: ExecutionContext) {
  type TValidateCallback = () => Future[ValidatorResponse]

  case class ValidationJob(id: String, categoryId: String, promise: Promise[ValidatorResponse], validate: TValidateCallback)

  private val maxConcurrentValidations = 4
  private val maxPendingValidations = 100
  private val pendingQueue = new ConcurrentLinkedQueue[ValidationJob]()
  private val currentValidations = new ConcurrentLinkedQueue[ValidationJob]()
  private val validators = new ConcurrentHashMap[String, (Category, Validator)]().asScala

  /**
    * Check the text with validators assigned to the given category ids.
    * If no ids are assigned, use all the currently available validators.
    */
  def check(id: String, text: String, maybeCategoryIds: Option[List[String]] = None): Future[List[RuleMatch]] = {
    val categoryIds = maybeCategoryIds match {
      case None => getCurrentCategories.map { case (_, category) => category.id }
      case Some(categoryIds) => categoryIds
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
    Logger.info(s"New instance of validator available of id: ${validator.getId} for category: ${category}")
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

    // Wrap our request in a lambda and add it to the queue, ready for processing
    val validate = () => validators.get(categoryId) match {
      case Some((_, validator)) =>
        blocking {
          val eventualResult = validator.check(ValidatorRequest(categoryId, text))
          eventualResult.andThen {
            case Success(result) => promise.success(result)
            case Failure(err) => promise.failure(err)
          }
        }
      case None =>
        Future.failed(new IllegalStateException(s"Could not run validation job with id: $id -- unknown category for id: $categoryId"))
    }

    scheduleValidationJob(ValidationJob(id, categoryId, promise, validate))

    future
  }

  private def completeValidationJob(job: ValidationJob): Unit = {
    currentValidations.remove(job)
    if (pendingQueue.size > 0) {
      val newJob = pendingQueue.remove()
      Logger.info(s"Starting new validation job from queue with id: ${job.id}. Remaining items in queue: ${pendingQueue.size}")
      scheduleValidationJob(newJob)
    }
    Logger.info(s"Validation job with id: ${job.id} for category: ${job.categoryId} is done. Current jobs: ${currentValidations.size}")
    Unit
  }

  private def scheduleValidationJob(job: ValidationJob) = {
    if (currentValidations.size <= maxConcurrentValidations) {
      Logger.info(s"Validation job with id: ${job.id} for category: ${job.categoryId} is processing")
      currentValidations.add(job)
      job.validate().andThen {
        case _ => completeValidationJob(job)
      }
    } else if (pendingQueue.size <= maxPendingValidations) {
      pendingQueue.add(job)
      Logger.info(s"Validation job stack full. Added job with id: ${job.id} to queue, which is now of size: ${pendingQueue.size}")
    } else {
      val errorMessage = s"Validation job queue full. Cannot accept new job with id: ${job.id} as queue is already of size: ${pendingQueue.size}"
      Logger.warn(errorMessage)
      job.promise.failure(new Exception(errorMessage))
    }
  }
}