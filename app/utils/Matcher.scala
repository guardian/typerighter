package utils

import model.{BaseRule, RuleMatch}
import services.MatcherRequest

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import model.Category

trait MatcherCompanion {
  def getType(): String
}

trait Matcher {
  def check(request: MatcherRequest)(implicit ec: ExecutionContext): Future[List[RuleMatch]]
  def getRules(): List[BaseRule]
  def getCategory(): Category
  def getType(): String
}
