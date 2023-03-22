package service

import com.gu.typerighter.lib.Loggable
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

object DbRuleManager extends Loggable {
  def baseRuleToDbRule(rule: BaseRule): DbRule = {
    rule match {
      case RegexRule(id, category, description, _, replacement, regex) =>
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

  def dbRuleToBaseRule(rule: DbRule): Either[String, BaseRule] = {
    rule match {
      case DbRule(
            _,
            "regex",
            pattern,
            replacement,
            Some(category),
            tags,
            description,
            ignore,
            notes,
            Some(googleSheetId),
            forceRedRule,
            advisoryRule
          ) =>
        Right(
          RegexRule(
            id = googleSheetId,
            category = Category(id = category, name = category),
            description = description.getOrElse(""),
            suggestions = List.empty,
            replacement = replacement.map(TextSuggestion(_)),
            regex = new ComparableRegex(pattern)
          )
        )
      case DbRule(
            _,
            "languageTool",
            pattern,
            replacement,
            Some(category),
            tags,
            description,
            ignore,
            notes,
            Some(googleSheetId),
            forceRedRule,
            advisoryRule
          ) =>
        Right(
          LTRuleXML(
            id = googleSheetId,
            category = Category(id = category, name = category),
            description = description.getOrElse(""),
            xml = pattern
          )
        )
      case other => Left(s"Could not derive BaseRule from DbRule for: $other")
    }
  }

  def destructivelyDumpRuleResourceToDB(rules: RuleResource): Either[List[String], RuleResource] = {
    rules.rules
      .map(baseRuleToDbRule)
      .grouped(100)
      .foreach(DbRule.batchInsert)

    val maybeAllDbRules = DbRule.findAll().map(dbRuleToBaseRule)
    val failedDbRules = maybeAllDbRules.collect { case l @ Left(_) => l }
    val successfulDbRules = maybeAllDbRules.collect { case r @ Right(_) => r.value }

    failedDbRules match {
      case Nil =>
        val persistedRules =
          RuleResource(rules = successfulDbRules, ltDefaultRuleIds = rules.ltDefaultRuleIds)

        if (persistedRules.rules != rules.rules) {
          val allRules = persistedRules.rules.zip(rules.rules)

          allRules
            .filter { case (persistedRule, expectedRule) => persistedRule != expectedRule }
            .take(10)
            .foreach { case (persistedRule, expectedRule) =>
              log.error(s"Persisted rule: $persistedRule")
              log.error(s"Expected rule: $expectedRule")
            }

          log.error(
            s"LT rule ids differ: ${persistedRules.ltDefaultRuleIds.diff(rules.ltDefaultRuleIds).mkString(",")}"
          )

          throw new Exception(
            s"Rules were persisted, but the persisted rules differ from the rules we received from the sheet."
          )
        }

        Right(persistedRules)
      case _ => Left(failedDbRules.map(_.value))
    }
  }
}
