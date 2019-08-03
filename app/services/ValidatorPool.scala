package services

import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise, blocking}
import play.api.Logger
import model.{Category, Rule, RuleMatch}
import utils.Validator
import utils.Validator._

import scala.collection.mutable.{ Queue, ListBuffer }

case class ValidatorConfig(rules: List[Rule])

case class ValidatorRequest(category: String, text: String)

trait ValidatorFactory {
  def createInstance(name: String, config: ValidatorConfig)(implicit ec: ExecutionContext): (Validator, List[String])
}

class ValidatorPool(implicit ec: ExecutionContext) {
  type TValidateCallback = () => Future[Promise[ValidatorResponse]]
  private val maxConcurrentValidations = 4
  private val maxPendingValidations = 100
  private val pendingQueue = new ConcurrentLinkedQueue[(Promise[ValidatorResponse], TValidateCallback)]()
  private val currentValidations = new ConcurrentLinkedQueue[Promise[ValidatorResponse]]()
  private val validators = new ConcurrentHashMap[String, (Category, Validator)]().asScala

  /**
    * Check the text with validators assigned to the given category ids.
    * If no ids are assigned, use all the currently available validators.
    */
  def check(text: String, maybeCategoryIds: Option[List[String]] = None): Future[List[RuleMatch]] = {
    val categoryIds = maybeCategoryIds match {
      case None => getCurrentCategories.map { case (_, category) => category.id }
      case Some(categoryIds) => categoryIds
    }

    val eventualChecks = categoryIds.map { categoryId =>
      Logger.info(s"Checking categories ${categoryIds.mkString(", ")}")
      checkRequest(ValidatorRequest(categoryId, text))
    }

    Future.sequence(eventualChecks).map {
      _.flatten
    }
  }

  /**
    * Add a validator to the pool of validators for the given category.
    * Replaces a validator that's already present for that category, returning
    * the replaced validator.
    */
  def addValidator(category: Category, validator: Validator): Option[(Category, Validator)] = {
    Logger.info(s"New instance of validator available with rules for category ${category}")
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

  private def checkRequest(request: ValidatorRequest): Future[ValidatorResponse] = {
    val promise = Promise[ValidatorResponse]()
    val future = promise.future

    // Wrap our request in a lambda and add it to the queue, ready for processing
    val validate: TValidateCallback = () => validators.get(request.category) match {
      case Some((_, validator)) =>
        Logger.info(s"Validator for category ${request.category} is processing")
        blocking {
          val eventualResult = validator.check(request)
          Logger.info(s"Validator for category ${request.category} is done")
          eventualResult.map { result =>
            // If it exists, begin the next queued validation
            completeValidationWork(promise)
            promise.success(result)
          }
        }
      case None =>
        completeValidationWork(promise)
        Future.failed(new IllegalStateException(s"Could not validate: unknown category ${request.category}"))
    }

    scheduleValidationWork(promise, validate)

    future
  }

  private def completeValidationWork(promise: Promise[ValidatorResponse]) = {
    currentValidations.remove(promise)
    if (pendingQueue.size > 0) {
      val (promise, validate) = pendingQueue.remove()
      Logger.info(s"Scheduling new work. Items in queue remain, which is now of size: ${pendingQueue.size}")
      scheduleValidationWork(promise, validate)
    }
  }

  private def scheduleValidationWork(promise: Promise[ValidatorResponse], validate: TValidateCallback) = {
    if (currentValidations.size <= maxConcurrentValidations) {
      currentValidations.add(promise)
      validate()
    } else if (pendingQueue.size <= maxPendingValidations) {
      pendingQueue.add((promise, validate))
      Logger.info(s"Validation stack full. Adding job to queue, which is now of size: ${pendingQueue.size}")
    } else {
      val errorMessage = s"Validation queue full. Cannot accept new validation as queue is already of size: ${pendingQueue.size}"
      Logger.warn(errorMessage)
      promise.failure(new Exception(errorMessage))
    }
  }
}