package services

import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise, blocking}

import play.api.Logger

import model.Rule
import utils.Validator
import utils.Validator._
import model.Category

case class ValidatorConfig(rules: List[Rule])

case class ValidatorRequest(category: String, text: String)

trait ValidatorFactory {
  def createInstance(name: String, config: ValidatorConfig)(implicit ec: ExecutionContext): (Validator, List[String])
}

class ValidatorPool(implicit ec: ExecutionContext) {

  private val validators = new ConcurrentHashMap[String, (Category, Promise[Validator])]().asScala

  def check(text: String, categoryIds: List[String]) = {
    Logger.info(s"Checking categories ${categoryIds.mkString(", ")}")
    val eventualChecks = categoryIds.map { categoryId =>
      checkRequest(ValidatorRequest(categoryId, text))
    }
    Future.sequence(eventualChecks).map {
      _.flatten
    }
  }

  private def checkRequest(request: ValidatorRequest): Future[ValidatorResponse] = {
    validators.get(request.category) match {
      case Some((_, validator)) =>
        Logger.info(s"Validator for category ${request.category} is processing")
        validator.future.flatMap { validator =>
          blocking {
            val result = validator.check(request)
            Logger.info(s"Validator for category ${request.category} is done")
            result
          }
        }
      case None =>
        Future.failed(new IllegalStateException(s"Unknown category ${request.category}"))
    }
  }

  def addValidator(category: Category, validator: Validator) = {
    Logger.info(s"New instance of validator available with rules for category ${category}")
    validators.put(category.id, (category, Promise.successful(validator)))
  }

  def getCurrentCategories: List[Category] = {
    validators.values.map { 
      case (category, _) => category
    }.toList
  }

  def getCurrentRules: Future[List[Rule]] = {
    val eventuallyValidatorRules = validators.values.map { 
      case (_, validatorPromise) => {
        validatorPromise.future.map { validator =>
          validator.getRules.toList
        }
      }
    }.toList
    Future.sequence(eventuallyValidatorRules).map(_.flatten)
  }
}