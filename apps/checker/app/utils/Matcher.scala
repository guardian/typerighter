package utils

import com.gu.typerighter.model.{CheckerRule, Category, RuleMatch}

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import services.MatcherRequest

trait MatcherCompanion {
  def getType(): String
}

trait Matcher {
  private val id = UUID.randomUUID().toString()

  def check(request: MatcherRequest)(implicit ec: ExecutionContext): Future[List[RuleMatch]]
  def getRules(): List[CheckerRule]
  def getCategories(): Set[Category]
  def getType(): String
  def getId() = id
}
