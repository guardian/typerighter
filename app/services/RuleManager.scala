package services

import model.PatternRule

import scala.concurrent.Future

/**
  * Manages CRUD operations for the Rule model.
  */
object RuleManager {
  private var ruleMap = Map[String, PatternRule]()

  def add(rule: PatternRule): Future[PatternRule] = {
    RuleManager.ruleMap = RuleManager.ruleMap + (rule.id -> rule)
    Future.successful(rule)
  }

  def remove(id: String): Future[Unit] = {
    RuleManager.ruleMap = RuleManager.ruleMap - id
    Future.successful()
  }

  def update(rule: PatternRule): Future[PatternRule] = {
    RuleManager.ruleMap = RuleManager.ruleMap + (rule.id -> rule)
    Future.successful(rule)
  }

  def get(id: String): Future[Option[PatternRule]] = {
    Future.successful(RuleManager.ruleMap.get(id))
  }

  def getAll(): Future[List[PatternRule]] = {
    Future.successful(RuleManager.ruleMap.toList.map(_._2))
  }
}