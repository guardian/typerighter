package service

import com.gu.typerighter.lib.Loggable
import com.gu.typerighter.model.{
  CheckerRule,
  Category,
  ComparableRegex,
  LTRule,
  LTRuleCore,
  LTRuleXML,
  RegexRule,
  CheckerRuleResource,
  TextSuggestion
}
import db.DraftDbRule
import db.DraftDbRule.autoSession
import scalikejdbc.DBSession

object DbRuleManager extends Loggable {
  object RuleType {
    val regex = "regex"
    val languageToolXML = "languageToolXML"
    val languageToolCore = "languageToolCore"
  }

  def checkerRuleToDbRule(rule: CheckerRule): DraftDbRule = {
    rule match {
      case RegexRule(id, category, description, _, replacement, regex) =>
        DraftDbRule.withUser(
          id = None,
          ruleType = RuleType.regex,
          pattern = Some(regex.toString()),
          category = Some(category.name),
          description = Some(description),
          replacement = replacement.map(_.text),
          googleSheetId = Some(id),
          user = "Google Sheet"
        )
      case LTRuleXML(id, xml, category, description) =>
        DraftDbRule.withUser(
          id = None,
          ruleType = RuleType.languageToolXML,
          pattern = Some(xml),
          category = Some(category.name),
          description = Some(description),
          replacement = None,
          googleSheetId = Some(id),
          user = "Google Sheet"
        )
      case LTRuleCore(_, languageToolRuleId) =>
        DraftDbRule.withUser(
          id = None,
          ruleType = RuleType.languageToolCore,
          googleSheetId = Some(languageToolRuleId),
          user = "Google Sheet"
        )
      case _: LTRule =>
        throw new Error(
          "A languageTool-generated rule should not be available in the context of the manager service"
        )
    }
  }

  def dbRuleToCheckerRule(rule: DraftDbRule): Either[String, CheckerRule] = {
    rule match {
      case DraftDbRule(
            _,
            RuleType.regex,
            Some(pattern),
            replacement,
            Some(category),
            _,
            description,
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
      case DraftDbRule(
            _,
            RuleType.languageToolXML,
            Some(pattern),
            _,
            Some(category),
            _,
            description,
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
      case DraftDbRule(
            _,
            RuleType.languageToolCore,
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

  def getRules()(implicit session: DBSession = autoSession): List[DraftDbRule] =
    DraftDbRule.findAll()

  def createCheckerRuleResourceFromDbRules(
      dbRules: List[DraftDbRule]
  ): Either[List[String], CheckerRuleResource] = {
    val (failedDbRules, successfulDbRules) = dbRules
      .filter(_.ignore == false)
      .map(dbRuleToCheckerRule)
      .partitionMap(identity)

    failedDbRules match {
      case Nil      => Right(CheckerRuleResource(successfulDbRules))
      case failures => Left(failures)
    }
  }

  def destructivelyDumpRulesToDB(
    incomingRules: List[DraftDbRule]
  ): Either[List[String], List[DraftDbRule]] = {
    DraftDbRule.destroyAll()

    incomingRules
      .grouped(100)
      .foreach(DraftDbRule.batchInsert)

    val persistedRules = getRules()
    val rulesToCompare = persistedRules.map(_.copy(id = None))

    if (rulesToCompare == incomingRules) {
      Right(persistedRules)
    } else {
      val allRules = rulesToCompare.zip(incomingRules)
      log.error(s"Persisted rules differ.")

      allRules.take(10).foreach { case (ruleToCompare, expectedRule) =>
        log.error(s"Persisted rule: $ruleToCompare")
        log.error(s"Expected rule: $expectedRule")
      }

      Left(
        List(
          s"Rules were persisted, but the persisted rules differ from the rules we received from the sheet."
        )
      )
    }
  }
}
