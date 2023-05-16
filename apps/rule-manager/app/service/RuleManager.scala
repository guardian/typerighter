package service

import com.gu.typerighter.lib.Loggable
import com.gu.typerighter.model.{
  CheckerRule,
  CheckerRuleResource,
  LTRule,
  LTRuleCore,
  LTRuleXML,
  RegexRule
}
import com.gu.typerighter.rules.BucketRuleResource
import db.{DbRuleDraft, DbRuleLive}
import db.DbRuleDraft.autoSession
import model.{LTRuleCoreForm, LTRuleXMLForm, RegexRuleForm}
import scalikejdbc.DBSession

import scala.util.Try

object RuleManager extends Loggable {
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

  def liveDbRuleToCheckerRule(rule: DbRuleLive): Either[String, CheckerRule] = {
    rule match {
      case r: DbRuleLive if r.ruleType == RuleType.regex =>
        RegexRuleForm.form
          .fill(
            (
              r.pattern.getOrElse(""),
              r.replacement,
              r.category.getOrElse(""),
              r.description.getOrElse(""),
              r.externalId.getOrElse(""),
            )
          )
          .fold(
            err => Left(err.errors.map(_.message).mkString(", ")),
            form => Right((RegexRuleForm.toRegexRule _).tupled(form))
          )
      case r: DbRuleLive if r.ruleType == RuleType.languageToolXML =>
        LTRuleXMLForm.form
          .fill(
            (
              r.pattern.getOrElse(""),
              r.category.getOrElse(""),
              r.description.getOrElse(""),
              r.externalId.getOrElse(""),
            )
          )
          .fold(
            err => Left(err.errors.map(_.message).mkString(", ")),
            form => Right((LTRuleXMLForm.toLTRuleXML _).tupled(form))
          )
      case r: DbRuleLive if r.ruleType == RuleType.languageToolXML =>
        LTRuleCoreForm.form
          .fill(
            r.externalId.getOrElse("")
          )
          .fold(
            err => Left(err.errors.map(_.message).mkString(", ")),
            form => Right(LTRuleCoreForm.toLTRuleCore(form))
          )
      case other => Left(s"Could not create a CheckerRule for the following DBRule: $other")
    }
  }

  def getDraftRules()(implicit session: DBSession = autoSession): List[DbRuleDraft] =
    DbRuleDraft.findAll()

  def publishRule(id: Int, user: String, reason: String, bucketRuleResource: BucketRuleResource)(
      implicit session: DBSession = autoSession
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
      liveRule = draftRule.toLive(reason)
      _ <- liveDbRuleToCheckerRule(liveRule).left.map(new Exception(_)).toTry
      persistedLiveRule <- DbRuleLive.create(liveRule, user)
      _ <- publishLiveRules(bucketRuleResource).left
        .map(errs => new Exception(errs.mkString(",")))
        .toTry
    } yield persistedLiveRule
  }

  def publishLiveRules(
      bucketRuleResource: BucketRuleResource
  ): Either[List[String], CheckerRuleResource] = {
    for {
      ruleResource <- getRuleResourceFromLiveRules()
      _ <- bucketRuleResource.putRules(ruleResource).left.map { l => List(l.toString) }
    } yield ruleResource
  }

  private def getRuleResourceFromLiveRules(
  ): Either[List[String], CheckerRuleResource] = {
    val (failedDbRules, successfulDbRules) =
      DbRuleLive
        .findAll()
        .map(liveDbRuleToCheckerRule)
        .partitionMap(identity)

    failedDbRules match {
      case Nil      => Right(CheckerRuleResource(successfulDbRules))
      case failures => Left(failures)
    }
  }

  def destructivelyPublishRules(
      incomingRules: List[DbRuleDraft],
      bucketRuleResource: BucketRuleResource
  ): Either[List[String], CheckerRuleResource] = {
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
      publishLiveRules(bucketRuleResource)
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
