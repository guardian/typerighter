package service

import com.gu.typerighter.model.{
  BaseRule,
  Category,
  ComparableRegex,
  LTRuleXML,
  RegexRule,
  RuleResource,
  TextSuggestion
}
import db.DbRule

object DbRuleManager {
  def baseRuleToDbRule(rule: BaseRule): DbRule = {
    rule match {
      case RegexRule(id, category, description, suggestions, replacement, regex) =>
        DbRule(
          id = None,
          ruleType = "regex",
          pattern = regex.toString(),
          category = Some(category.name),
          description = Some(description),
          replacement = replacement.map(_.text),
          ignore = false,
          googleSheetId = Some(id)
        )
      case LTRuleXML(id, xml, category, description) =>
        DbRule(
          id = None,
          ruleType = "languageTool",
          pattern = xml,
          category = Some(category.name),
          description = Some(description),
          replacement = None,
          ignore = false,
          googleSheetId = Some(id)
        )
    }
  }

  def dbRuleToBaseRule(rule: DbRule): BaseRule = {
    rule.ruleType match {
      case "regex" =>
        RegexRule(
          id = rule.googleSheetId.get,
          category = Category(id = rule.category.get, name = rule.category.get),
          description = rule.description.get,
          suggestions = List.empty,
          replacement = rule.replacement.map(TextSuggestion(_)),
          regex = new ComparableRegex(rule.pattern)
        )
      case "languageTool" =>
        LTRuleXML(
          id = rule.googleSheetId.get,
          category = Category(id = rule.category.get, name = rule.category.get),
          description = rule.description.get,
          xml = rule.pattern
        )
    }
  }

  def overwriteAllRules(rules: RuleResource): RuleResource = {
    rules.rules
      .map(baseRuleToDbRule)
      .grouped(100)
      .foreach(DbRule.batchInsert)

    val dbRules = DbRule.findAll().map(dbRuleToBaseRule)
    val persistedRules = RuleResource(rules = dbRules, ltDefaultRuleIds = List.empty)
    val expectedRules = rules.copy(ltDefaultRuleIds = List.empty)

    if (persistedRules.rules != expectedRules.rules) {
      val allRules = persistedRules.rules.zip(expectedRules.rules)

      allRules
        .filter { case (persistedRule, expectedRule) => persistedRule != expectedRule }
        .take(10)
        .foreach { case (persistedRule, expectedRule) =>
          println(s"Persisted rule: $persistedRule")
          println(s"Expected rule: $expectedRule")
        }

      throw new Exception(
        s"Rules were persisted, but the persisted rules differ from the rules we received from the sheet."
      )
    }
    persistedRules
  }
}
