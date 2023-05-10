package service

import com.gu.typerighter.lib.Loggable
import com.gu.typerighter.model.{
  Category,
  CheckerRule,
  CheckerRuleResource,
  ComparableRegex,
  LTRule,
  LTRuleCore,
  LTRuleXML,
  RegexRule,
  TextSuggestion
}
import db.{DbRuleDraft, DbRuleLive}
import db.DbRuleDraft.autoSession
import scalikejdbc.DBSession

import scala.util.Try

object RuleManagement extends Loggable {
  object RuleType {
    val regex = "regex"
    val languageToolXML = "languageToolXML"
    val languageToolCore = "languageToolCore"
  }

  def checkerRuleToDraftDbRule(rule: CheckerRule): DbRuleDraft = {
    rule match {
      case RegexRule(id, category, description, _, replacement, regex) =>
        DbRuleDraft.withUser(
          id = None,
          ruleType = RuleType.regex,
          pattern = Some(regex.toString()),
          category = Some(category.name),
          description = Some(description),
          ignore = false,
          replacement = replacement.map(_.text),
          externalId = Some(id),
          user = "Google Sheet"
        )
      case LTRuleXML(id, xml, category, description) =>
        DbRuleDraft.withUser(
          id = None,
          ruleType = RuleType.languageToolXML,
          pattern = Some(xml),
          category = Some(category.name),
          description = Some(description),
          ignore = false,
          replacement = None,
          externalId = Some(id),
          user = "Google Sheet"
        )
      case LTRuleCore(_, languageToolRuleId) =>
        DbRuleDraft.withUser(
          id = None,
          ruleType = RuleType.languageToolCore,
          externalId = Some(languageToolRuleId),
          ignore = false,
          user = "Google Sheet"
        )
      case _: LTRule =>
        throw new Error(
          "A languageTool-generated rule should not be available in the context of the manager service"
        )
    }
  }

  def draftDbRuleToCheckerRule(rule: DbRuleDraft): Either[String, CheckerRule] = {
    rule match {
      case DbRuleDraft(
            _,
            RuleType.regex,
            Some(pattern),
            replacement,
            Some(category),
            _,
            description,
            _,
            _,
            Some(externalId),
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
            id = externalId,
            category = Category(id = category, name = category),
            description = description.getOrElse(""),
            suggestions = List.empty,
            replacement = replacement.map(TextSuggestion(_)),
            regex = new ComparableRegex(pattern)
          )
        )
      case DbRuleDraft(
            _,
            RuleType.languageToolXML,
            Some(pattern),
            _,
            Some(category),
            _,
            description,
            _,
            _,
            Some(externalId),
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
            id = externalId,
            category = Category(id = category, name = category),
            description = description.getOrElse(""),
            xml = pattern
          )
        )
      case DbRuleDraft(
            _,
            RuleType.languageToolCore,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            Some(externalId),
            _,
            _,
            _,
            _,
            _,
            _,
            _
          ) =>
        Right(LTRuleCore(externalId, externalId))
      case other => Left(s"Could not create a CheckerRule for the following DBRule: $other")
    }
  }

  def getDraftRules()(implicit session: DBSession = autoSession): List[DbRuleDraft] =
    DbRuleDraft.findAll()

  def publishRule(id: Int, user: String, reason: String)(implicit
      session: DBSession = autoSession
  ): Try[DbRuleLive] = {
    for {
      draftRule <- DbRuleDraft
        .find(id)
        .toRight(
          new Exception(
            s"Attempted to publish rule with id $id for user $user, but could not find a draft rule with that id"
          )
        )
        .toTry
      _ <- draftDbRuleToCheckerRule(draftRule).left.map(new Exception(_)).toTry
      liveRule <- DbRuleLive.create(draftRule.toLive(reason), user)
    } yield liveRule
  }

  def createCheckerRuleResourceFromDbRules(
      dbRules: List[DbRuleDraft]
  ): Either[List[String], CheckerRuleResource] = {
    val (failedDbRules, successfulDbRules) = dbRules
      .filter(_.ignore == false)
      .map(draftDbRuleToCheckerRule)
      .partitionMap(identity)

    failedDbRules match {
      case Nil      => Right(CheckerRuleResource(successfulDbRules))
      case failures => Left(failures)
    }
  }

  def destructivelyDumpRulesToDB(
      incomingRules: List[DbRuleDraft]
  ): Either[List[String], List[DbRuleDraft]] = {
    DbRuleDraft.destroyAll()
    DbRuleLive.destroyAll()

    incomingRules
      .grouped(100)
      .foreach(DbRuleDraft.batchInsert)

    val liveRules = incomingRules.filterNot(_.ignore).map(_.toLive("Imported from Google Sheet"))

    liveRules
      .grouped(100)
      .foreach(DbRuleLive.batchInsert)

    val persistedRules = getDraftRules()
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
