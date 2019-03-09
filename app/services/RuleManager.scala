package services

import model.PatternRule
import play.api.Configuration

import scala.concurrent.Future

/**
  * Manages CRUD persist operations for the Rule model.
  */
class RuleManager(config: Configuration) {
  def fetchByCategory(): Future[(Map[String, List[PatternRule]], List[String])] = {
    val (rules, errors) = SheetsResource.getDictionariesFromSheet(config)
    val rulesByCategory = rules.foldLeft(Map[String, List[PatternRule]]())((acc, rule) => {
      acc.get(rule.category.id) match {
        case Some(ruleList) => acc + (rule.category.id -> (ruleList :+ rule))
        case None => acc + (rule.category.id -> List(rule))
      }
    })
    Future.successful(rulesByCategory, errors)
  }
}