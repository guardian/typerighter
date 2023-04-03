package service

import com.gu.typerighter.lib.Loggable
import com.gu.typerighter.model.{
  BaseRule,
  Category,
  ComparableRegex,
  LTRuleCore,
  LTRuleXML,
  RegexRule,
  RuleResource,
  TextSuggestion
}
import db.DbRule

object DbRuleManager extends Loggable {
  object RuleType {
    val regex = "regex"
    val languageToolXML = "languageToolXML"
    val languageToolCore = "languageToolCore"
  }

  def baseRuleToDbRule(rule: BaseRule): DbRule = {
    rule match {
      case RegexRule(id, category, description, _, replacement, regex) =>
        DbRule(
          id = None,
          ruleType = RuleType.regex,
          pattern = Some(regex.toString()),
          category = Some(category.name),
          description = Some(description),
          replacement = replacement.map(_.text),
          ignore = false,
          googleSheetId = Some(id)
        )
      case LTRuleXML(id, xml, category, description) =>
        DbRule(
          id = None,
          ruleType = RuleType.languageToolXML,
          pattern = Some(xml),
          category = Some(category.name),
          description = Some(description),
          replacement = None,
          ignore = false,
          googleSheetId = Some(id)
        )
      case LTRuleCore(_, languageToolRuleId) =>
        DbRule(
          id = None,
          ruleType = RuleType.languageToolCore,
          googleSheetId = Some(languageToolRuleId),
          ignore = false
        )
    }
  }

  def dbRuleToBaseRule(rule: DbRule): Either[String, BaseRule] = {
    rule match {
      case DbRule(
            _,
            RuleType.regex,
            Some(pattern),
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
            RuleType.languageToolXML,
            Some(pattern),
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
      case DbRule(_, RuleType.languageToolCore, _, _, _, _, _, _, _, Some(googleSheetId), _, _) =>
        Right(LTRuleCore(googleSheetId, googleSheetId))
      case other => Left(s"Could not derive BaseRule from DbRule for: $other")
    }
  }

  def destructivelyDumpRuleResourceToDB(rules: List[DbRule]): Either[List[String], RuleResource] = {
    DbRule.destroyAll()

    rules
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

        if (persistedRules.rules == rules) {
          Right(persistedRules)
        } else {
          val allRules = persistedRules.rules.zip(rules)
          log.error(s"Persisted rules differ.")
          val diffRules = allRules
            .filter { case (persistedRule, expectedRule) => persistedRule != expectedRule }

          allRules.take(10).foreach { case (persistedRule, expectedRule) =>
            log.error(s"Persisted rule: $persistedRule")
            log.error(s"Expected rule: $expectedRule")
          }

          log.info((persistedRules.rules == rules).toString)
          log.info((persistedRules == rules).toString)

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
