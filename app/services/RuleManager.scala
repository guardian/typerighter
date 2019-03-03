package services

import model.PatternRule
import play.api.Configuration

import scala.concurrent.Future

/**
  * Manages CRUD persist operations for the Rule model.
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

  def fetchAll(): Future[(List[PatternRule], List[String])] = {
    Future.successful(SheetsResource.getDictionariesFromSheet(config))
  }
}