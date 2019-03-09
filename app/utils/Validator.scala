package utils

import model.{PatternRule, RuleMatch}
import services.ValidatorRequest

object Validator {
  type ValidatorResponse = List[RuleMatch]
}

trait Validator {
  def check(request: ValidatorRequest): Validator.ValidatorResponse
  def getRules(): List[PatternRule]
  def getCategory(): String
}
