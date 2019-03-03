package services

import model.PatternRule
import play.api.Configuration

import scala.concurrent.Future

/**
  * Manages CRUD operations for the Rule model.
  */
class RuleManager(config: Configuration) {
  var ruleMap = Map[String, PatternRule]()

  def add(rule: PatternRule): Future[PatternRule] = {
    ruleMap = ruleMap + (rule.id -> rule)
    Future.successful(rule)
  }

  def remove(id: String): Future[Unit] = {
    ruleMap = ruleMap - id
    Future.successful()
  }

  def update(rule: PatternRule): Future[PatternRule] = {
    ruleMap = ruleMap + (rule.id -> rule)
    Future.successful(rule)
  }

  def get(id: String): Future[Option[PatternRule]] = {
    Future.successful(ruleMap.get(id))
  }

  def getAll(): Future[List[PatternRule]] = {
    val maybeRules = SheetsResource.getDictionariesFromSheet(config).map {
      case rules: List[PatternRule] => Future.successful(rules)
    }
    maybeRules.getOrElse(Future.successful(Nil))
  }
}