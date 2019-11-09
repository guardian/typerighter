package utils

import model.{Rule, RuleMatch}
import services.MatcherRequest

import scala.concurrent.Future

trait Matcher {
  def check(request: MatcherRequest): Future[List[RuleMatch]]
  def getId(): String
  def getRules(): List[Rule]
  def getCategory(): String
}
