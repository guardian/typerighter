package utils

import model.{BaseRule, RuleMatch}
import services.MatcherRequest

import scala.concurrent.Future

trait Matcher {
  def check(request: MatcherRequest): Future[List[RuleMatch]]
  def getId(): String
  def getRules(): List[BaseRule]
  def getCategory(): String
}
