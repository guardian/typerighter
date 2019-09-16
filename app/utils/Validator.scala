package utils

import model.{Rule, RuleMatch}
import services.ValidatorRequest

import scala.concurrent.Future

trait Validator {
  def check(request: ValidatorRequest): Future[List[RuleMatch]]
  def getId(): String
  def getRules(): List[Rule]
  def getCategory(): String
}
