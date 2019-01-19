package services

import model.PatternRule

/**
  * Manages CRUD operations for the Rule model.
  */
object RuleManager {
  var ruleMap = Map[String, PatternRule]()

  def add(rule: PatternRule): Unit = {
    RuleManager.ruleMap = RuleManager.ruleMap + (rule.id -> rule)
  }

  def remove(id: String): Unit = {
    RuleManager.ruleMap = RuleManager.ruleMap - id
  }

  def update(rule: PatternRule): Unit = {
    RuleManager.ruleMap = RuleManager.ruleMap + (rule.id -> rule)
  }

  def get(id: String): Option[PatternRule] = {
    RuleManager.ruleMap.get(id)
  }

  def getAll(): List[PatternRule] = {
    RuleManager.ruleMap.toList.map(_._2)
  }
}