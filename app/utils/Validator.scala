package utils

import model.{Rule, RuleMatch}
import services.ValidatorRequest

import scala.concurrent.Future

object Validator {
  type ValidatorResponse = List[RuleMatch]
}

trait Validator {
  def check(request: ValidatorRequest): Future[Validator.ValidatorResponse]
  def getRules(): List[Rule]
  def getCategory(): String
}
