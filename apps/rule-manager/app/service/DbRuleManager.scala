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
            _,
            description,
            _,
            _,
            Some(googleSheetId),
            _,
            _
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
            _,
            Some(category),
            _,
            description,
            _,
            _,
            Some(googleSheetId),
            _,
            _
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
    DbRule.destroyAll()

    rules.rules
      .map(baseRuleToDbRule)
      .grouped(100)
      .foreach(DbRule.batchInsert)

    val maybeAllDbRules = DbRule.findAll().map(dbRuleToBaseRule)

    val (failedDbRules, successfulDbRules) = maybeAllDbRules.partitionMap {
      case l @ Left(_)  => l
      case r @ Right(_) => r
    }

    failedDbRules match {
      case Nil =>
        val persistedRules =
          RuleResource(rules = successfulDbRules)

        if (persistedRules.rules == rules.rules) {
          Right(persistedRules)
        } else {
          val allRules = persistedRules.rules.zip(rules.rules)

          allRules
            .filter { case (persistedRule, expectedRule) => persistedRule != expectedRule }
            .take(10)
            .foreach { case (persistedRule, expectedRule) =>
              log.error(s"Persisted rule: $persistedRule")
              log.error(s"Expected rule: $expectedRule")
            }

          Left(
            List(
              s"Rules were persisted, but the persisted rules differ from the rules we received from the sheet."
            )
          )
        }
      case _ => Left(failedDbRules)
    }
  }
}
