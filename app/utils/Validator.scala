package utils

import model.{Rule, RuleMatch}
import services.ValidatorRequest

object Validator {
  type ValidatorResponse = List[RuleMatch]
}

trait Validator {
  def check(request: ValidatorRequest): Validator.ValidatorResponse
  def getRules(): List[Rule]
  def getCategory(): String
}
