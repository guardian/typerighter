package rules

import scala.concurrent.Future
import model.{Category, Rule}

trait RuleResource {
  type Error = String
  def fetchRulesByCategory(): Future[(Map[Category, List[Rule]], List[Error])]
}