package utils

import java.util.UUID

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import services.MatcherRequest
import model.{BaseRule, RuleMatch}
import model.Category

trait MatcherCompanion {
  def getType(): String
}

trait Matcher {
  private val id = UUID.randomUUID().toString()

  def check(request: MatcherRequest)(implicit ec: ExecutionContext): Future[List[RuleMatch]]
  def getRules(): List[BaseRule]
  def getCategory(): Category
  def getType(): String
  def getId() = id
}
