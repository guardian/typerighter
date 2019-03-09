package services

import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise, blocking}
import model.{PatternRule, RuleMatch}
import play.api.Logger
import utils.Validator
import utils.Validator._

import scala.util.Success

case class ValidatorConfig(rules: List[PatternRule])
case class ValidatorRequest(category: String, text: String)

trait ValidatorFactory {
  def createInstance(name: String, config: ValidatorConfig)(implicit ec: ExecutionContext): (Validator, List[String])
}

class ValidatorPool(factory: ValidatorFactory)(implicit ec: ExecutionContext) {

  private val validators = new ConcurrentHashMap[String, Promise[Validator]]().asScala

  def checkAllCategories(text: String): Future[ValidatorResponse] = {
    Logger.info(s"Checking categories ${validators.keys.mkString}")
    val eventualChecks = validators.keys.foldLeft[List[Future[ValidatorResponse]]](List.empty)((acc, category) => {
      acc :+ check(ValidatorRequest(category, text))
    })
    Future.sequence(eventualChecks).map { _.flatten }
  }

  def check(request: ValidatorRequest): Future[ValidatorResponse] = {
    validators.get(request.category) match {
      case Some(validator) =>
        Logger.info(s"Validator for category ${request.category} is processing")
        validator.future.map { validator =>
          blocking {
            validator.check(request)
          }
        }

      case None =>
        Future.failed(new IllegalStateException(s"Unknown category ${request.category}"))
    }
  }

  def updateConfig(category: String, config: ValidatorConfig): Future[List[String]] = {
    Future {
      blocking {
        factory.createInstance(category, config)
      }
    }.map {
      case (validator, errors) =>
        Logger.info(s"New instance of validator available with rules for category ${category}")
        validators.put(category, Promise.successful(validator))
        errors
    }
  }

  def getCurrentRules = Future.sequence(validators.values.map(_.future).map(_.map(_.getRules())).toList).map(_.flatten)
}