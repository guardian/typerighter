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
import play.api.libs.json.Json
import scalikejdbc.DBSession

object AllRuleData {
  implicit val writes = Json.writes[AllRuleData]
}

/* All the data associated with a rule, including the current draft rule, the active
 * live rule if present, and all previous published versions of the rule.
 */
case class AllRuleData(
    draft: DbRuleDraft,
    live: Seq[DbRuleLive]
)

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

  def getAllRuleData(id: Int)(implicit
      session: DBSession = autoSession
  ): Option[AllRuleData] = {
    DbRuleDraft.find(id).map { draftRule =>
      val liveRules = draftRule.externalId match {
        case Some(externalId) => DbRuleLive.find(externalId).sortBy(-_.revisionId)
        case None             => List.empty
      }

      AllRuleData(draftRule, liveRules)
    }
  }

  def publishRule(id: Int, user: String, reason: String, bucketRuleResource: BucketRuleResource)(
      implicit session: DBSession = autoSession
  ): Either[Seq[FormError], AllRuleData] = {
    for {
      liveRule <- parseDraftRuleForPublication(id, reason)
      _ <- DbRuleLive
        .create(liveRule, user)
        .toEither
        .left
        .map(e => Seq(FormError("Error writing rule to live table", e.getMessage)))
      _ <- publishLiveRules(bucketRuleResource)
      allRuleData <- getAllRuleData(id)
        .toRight(Seq(FormError("Error reading rule from live table", "Rule not found")))
    } yield allRuleData
  }

  /** Given a draft rule and a reason for publication, validates the draft rule for publication and
    * returns a valid live rule.
    */
  def parseDraftRuleForPublication(id: Int, reason: String) = {
    for {
      draftRule <- DbRuleDraft
        .find(id)
        .toRight(
          Seq(
            FormError(
              "Finding existing rule",
              s"Could not find a draft rule with id $id"
            )
          )
        )
      liveRule = draftRule.toLive(reason)
      externalId <- liveRule.externalId.toRight(Seq(FormError("External id", "required")))
      _ <- liveDbRuleToCheckerRule(liveRule)
      _ <- DbRuleLive
        .findRevision(externalId, draftRule.revisionId)
        .map(_ =>
          Seq(
            FormError(
              "Rule already exists",
              "the current draft rule has not changed since it was last published"
            )
          )
        )
        .toLeft(())
    } yield liveRule
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
        .findAllActive()
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

    val draftRules = DbRuleDraft.findAll()

    val liveRules = draftRules
      .filterNot(_.ignore)
      .map(_.toLive("Imported from Google Sheet").copy(isActive = true))

    liveRules
      .grouped(100)
      .foreach(DbRuleLive.batchInsert)

    val persistedRules = getDraftRules()

    val persistedRulesToCompare = persistedRules.map(_.copy(id = None))
    val incomingRulesToCompare =
      incomingRules.map(rule => rule.copy(id = None, isPublished = !rule.ignore))

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

  def unpublishRule(
      id: Int,
      user: String,
      bucketRuleResource: BucketRuleResource
  ): Either[Exception, (Option[DbRuleDraft], Option[DbRuleLive])] = {
    try {
      val maybeDraftRule = DbRuleDraft.find(id)

      val maybeUpdatedLiveRule = for {
        draftRule <- maybeDraftRule
        externalId <- draftRule.externalId
        liveRule <- DbRuleLive.findLatestRevision(externalId)
        if liveRule.isActive
        updatedLiveRule <- DbRuleLive.setInactive(externalId, user)
        _ <- publishLiveRules(bucketRuleResource).toOption
      } yield updatedLiveRule

      // The draft rule needs to be updated to reflect the fact that it is no longer published
      // This also allows us to republish without editing the rule
      val maybeUpdatedDraftRule = for {
        _ <- maybeUpdatedLiveRule
        draftRule <- maybeDraftRule
        updatedDraftRule <- DbRuleDraft.save(draftRule, user).toOption
      } yield updatedDraftRule

      Right(maybeUpdatedDraftRule, maybeUpdatedLiveRule)
    } catch {
      case e: Exception => Left(e)
    }
  }

  def archiveRule(
      id: Int,
      user: String
  ): Either[Exception, Option[DbRuleDraft]] = {
    try {
      val maybeDraftRule = DbRuleDraft.find(id)

      val maybeUpdatedDraftRule = for {
        draftRule <- maybeDraftRule
        if !draftRule.isArchived
        archivedDraftRule = draftRule.copy(isArchived = true)
        updatedDraftRule <- DbRuleDraft.save(archivedDraftRule, user).toOption
      } yield updatedDraftRule

      Right(maybeUpdatedDraftRule)
    } catch {
      case e: Exception => Left(e)
    }
  }

  def unarchiveRule(
      id: Int,
      user: String
  ): Either[Exception, Option[DbRuleDraft]] = {
    try {
      val maybeDraftRule = DbRuleDraft.find(id)

      val maybeUpdatedDraftRule = for {
        draftRule <- maybeDraftRule
        if draftRule.isArchived
        archivedDraftRule = draftRule.copy(isArchived = false)
        updatedDraftRule <- DbRuleDraft.save(archivedDraftRule, user).toOption
      } yield updatedDraftRule

      Right(maybeUpdatedDraftRule)
    } catch {
      case e: Exception => Left(e)
    }
  }
}
