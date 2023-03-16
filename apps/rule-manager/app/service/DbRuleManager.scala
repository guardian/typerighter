package service

import com.gu.typerighter.model.{BaseRule, RegexRule, RuleResource}
import db.DbRule

class DbRuleManager {
  def baseRuleToDbRule(rule: BaseRule): DbRule = {
    rule match {
      case RegexRule(id, category, description, suggestions, replacement, regex) => DbRule(
        id = 1,
        ruleType = "regex",
        pattern = regex.toString(),
        category = Some(category.name),
        description = Some(description),
        replacement = replacement.map(_.text),
        ignore = false,
      )
    }
  }

  def overwriteAllRules(rules: RuleResource): RuleResource = {
//    DbRule.batchInsert(rules.rules)
  }
}
