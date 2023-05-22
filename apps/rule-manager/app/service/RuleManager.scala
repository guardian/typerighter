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
import play.api.data.FormError
import scalikejdbc.DBSession

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

  def liveDbRuleToCheckerRule(rule: DbRuleLive): Either[Seq[FormError], CheckerRule] = {
    rule match {
      case r: DbRuleLive if r.ruleType == RuleType.regex =>
        RegexRuleForm.form
          .fillAndValidate(
            (
              r.pattern.getOrElse(""),
              r.replacement,
              r.category.getOrElse(""),
              r.description.getOrElse(""),
              r.externalId.getOrElse(""),
            )
          )
          .fold(
            err => Left(err.errors),
            form => Right((RegexRuleForm.toRegexRule _).tupled(form))
          )
      case r: DbRuleLive if r.ruleType == RuleType.languageToolXML =>
        LTRuleXMLForm.form
          .fillAndValidate(
            (
              r.pattern.getOrElse(""),
              r.category.getOrElse(""),
              r.description.getOrElse(""),
              r.externalId.getOrElse(""),
            )
          )
          .fold(
            err => Left(err.errors),
            form => Right((LTRuleXMLForm.toLTRuleXML _).tupled(form))
          )
      case r: DbRuleLive if r.ruleType == RuleType.languageToolCore =>
        LTRuleCoreForm.form
          .fillAndValidate(
            r.externalId.getOrElse("")
          )
          .fold(
            err => Left(err.errors),
            form => Right(LTRuleCoreForm.toLTRuleCore(form))
          )
      case other =>
        Left(
          Seq(
            FormError(
              s"Could not create a CheckerRule for DBRule $other",
              "Rule type not recognised"
            )
          )
        )
    }
  }

  def getDraftRules()(implicit session: DBSession = autoSession): List[DbRuleDraft] =
    DbRuleDraft.findAll()

  def publishRule(id: Int, user: String, reason: String, bucketRuleResource: BucketRuleResource)(
      implicit session: DBSession = autoSession
  ): Either[Seq[FormError], DbRuleLive] = {
    for {
      draftRule <- DbRuleDraft
        .find(id)
        .toRight(
          Seq(
            FormError(
              "Finding existing rule",
              s"Attempted to publish rule with id $id for user $user, but could not find a draft rule with that id"
            )
          )
        )
      liveRule = draftRule.toLive(reason)
      _ <- liveDbRuleToCheckerRule(liveRule)
      persistedLiveRule <- DbRuleLive
        .create(liveRule, user)
        .toEither
        .left
        .map(e => Seq(FormError("Error writing rule to live table", e.getMessage)))
      _ <- publishLiveRules(bucketRuleResource)
    } yield persistedLiveRule
  }

  def publishLiveRules(
      bucketRuleResource: BucketRuleResource
  ): Either[Seq[FormError], CheckerRuleResource] = {
    for {
      ruleResource <- getRuleResourceFromLiveRules()
      _ <- bucketRuleResource.putRules(ruleResource).left.map { l =>
        Seq(FormError("Error writing rules to the artefact bucket", l.toString))
      }
    } yield ruleResource
  }

  private def getRuleResourceFromLiveRules(
  ): Either[Seq[FormError], CheckerRuleResource] = {
    val (failedDbRules, successfulDbRules) =
      DbRuleLive
        .findAll()
        .map(liveDbRuleToCheckerRule)
        .partitionMap(identity)

    failedDbRules match {
      case Nil      => Right(CheckerRuleResource(successfulDbRules))
      case failures => Left(failures.flatten)
    }
  }

  def destructivelyPublishRules(
      incomingRules: List[DbRuleDraft],
      bucketRuleResource: BucketRuleResource
  ): Either[Seq[FormError], CheckerRuleResource] = {
    DbRuleDraft.destroyAll()
    DbRuleLive.destroyAll()

    incomingRules
      .grouped(100)
      .foreach(DbRuleDraft.batchInsert)

    val liveRules = incomingRules
      .filterNot(_.ignore)
      .map(_.toLive("Imported from Google Sheet").copy(isActive = true))

    liveRules
      .grouped(100)
      .foreach(DbRuleLive.batchInsert)

    val persistedRules = getDraftRules()

    val persistedRulesToCompare = persistedRules.map(_.copy(id = None))
    val incomingRulesToCompare = incomingRules.map(_.copy(id = None))

    if (persistedRulesToCompare == incomingRulesToCompare) {
      publishLiveRules(bucketRuleResource)
    } else {
      val allRules = persistedRulesToCompare.zip(incomingRulesToCompare)
      log.error(s"Persisted rules differ.")

      allRules.take(10).foreach { case (ruleToCompare, expectedRule) =>
        log.error(s"Persisted rule: $ruleToCompare")
        log.error(s"Expected rule: $expectedRule")
      }

      Left(
        List(
          FormError(
            "Error publishing rules",
            s"Rules were written to the live table, but the persisted rules differ from the rules we received from the sheet."
          )
        )
      )
    }
  }
}
