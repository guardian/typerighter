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
        DbRule.withUser(
          id = None,
          ruleType = RuleType.regex,
          pattern = Some(regex.toString()),
          category = Some(category.name),
          description = Some(description),
          replacement = replacement.map(_.text),
          ignore = false,
          googleSheetId = Some(id),
          user = "Google Sheet"
        )
      case LTRuleXML(id, xml, category, description) =>
        DbRule.withUser(
          id = None,
          ruleType = RuleType.languageToolXML,
          pattern = Some(xml),
          category = Some(category.name),
          description = Some(description),
          replacement = None,
          ignore = false,
          googleSheetId = Some(id),
          user = "Google Sheet"
        )
      case LTRuleCore(_, languageToolRuleId) =>
        DbRule.withUser(
          id = None,
          ruleType = RuleType.languageToolCore,
          googleSheetId = Some(languageToolRuleId),
          ignore = false,
          user = "Google Sheet"
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
            _,
            _,
            _,
            _,
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
            _,
            _,
            _,
            _,
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
      case DbRule(
            _,
            RuleType.languageToolCore,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            Some(googleSheetId),
            _,
            _,
            _,
            _,
            _,
            _,
            _
          ) =>
        Right(LTRuleCore(googleSheetId, googleSheetId))
      case other => Left(s"Could not derive BaseRule from DbRule for: $other")
    }
  }

  def getRules(): List[DbRule] = DbRule.findAll()

  def getRulesAsRuleResource() = {
    val (failedDbRules, successfulDbRules) = getRules()
      .map(dbRuleToBaseRule)
      .partitionMap(identity)

    failedDbRules match {
      case Nil      => Right(RuleResource(successfulDbRules))
      case failures => Left(failures)
    }
  }

  def destructivelyDumpRuleResourceToDB(rules: RuleResource): Either[List[String], RuleResource] = {
    DbRule.destroyAll()

    rules.rules
      .map(baseRuleToDbRule)
      .grouped(100)
      .foreach(DbRule.batchInsert)

    val maybeAllDbRules = getRulesAsRuleResource()

    maybeAllDbRules.fold(
      Left(_),
      persistedRules => {
        if (persistedRules.rules == rules.rules) {
          Right(persistedRules)
        } else {
          val allRules = persistedRules.rules.zip(rules.rules)
          log.error(s"Persisted rules differ.")

          allRules.take(10).foreach { case (persistedRule, expectedRule) =>
            log.error(s"Persisted rule: $persistedRule")
            log.error(s"Expected rule: $expectedRule")
          }

          log.info((persistedRules.rules == rules.rules).toString)
          log.info((persistedRules == rules).toString)

          Left(
            List(
              s"Rules were persisted, but the persisted rules differ from the rules we received from the sheet."
            )
          )
        }
      }
    )
  }
}
