package rules

import model.{Category, PatternRule}
import models.Category

import scala.concurrent.Future

trait RuleResource {
  def fetchByCategory(): Future[(Map[Category, List[PatternRule]], List[String])]
}