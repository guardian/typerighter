package utils

import model.{BaseRule, RuleMatch}
import services.MatcherRequest

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import model.Category

trait Matcher {
  def check(request: MatcherRequest)(implicit ec: ExecutionContext): Future[List[RuleMatch]]
  def getType(): String
  def getRules(): List[BaseRule]
  def getCategory(): Category
}
