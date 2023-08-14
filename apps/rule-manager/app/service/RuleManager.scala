package service

import com.gu.typerighter.lib.Loggable
import com.gu.typerighter.model.{
  CheckerRule,
  CheckerRuleResource,
  DictionaryRule,
  LTRule,
  LTRuleCore,
  LTRuleXML,
  RegexRule
}
import com.gu.typerighter.rules.BucketRuleResource
import db.{DbRuleDraft, DbRuleLive, RuleTagDraft, RuleTagLive}
import db.DbRuleDraft.autoSession
import model.{DictionaryForm, LTRuleCoreForm, LTRuleXMLForm, PaginatedResponse, RegexRuleForm}
import play.api.data.FormError
import play.api.libs.json.{Json, OWrites}
import scalikejdbc.DBSession

object AllRuleData {
  implicit val writes: OWrites[AllRuleData] = Json.writes[AllRuleData]
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
    val dictionary = "dictionary"
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
          user = "Google Sheet",
          ruleOrder = 0
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
          user = "Google Sheet",
          ruleOrder = 0
        )
      case LTRuleCore(_, languageToolRuleId) =>
        DbRuleDraft.withUser(
          id = None,
          ruleType = RuleType.languageToolCore,
          externalId = Some(languageToolRuleId),
          ignore = false,
          user = "Google Sheet",
          replacement = None,
          ruleOrder = 0
        )
      case DictionaryRule(id, word, category) =>
        DbRuleDraft.withUser(
          id = None,
          pattern = Some(word),
          ruleType = RuleType.dictionary,
          category = Some(category.name),
          externalId = Some(id),
          ignore = false,
          user = "Google Sheet",
          ruleOrder = 0
        )
      case _: LTRule =>
        throw new Error(
          "A languageTool-generated rule should not be available in the context of the manager service"
        )
    }
  }

  def camelToKebab: String => String = _.foldLeft("") {
    case (acc, chr) if chr.isUpper => acc :+ '-' :+ chr.toLower
    case (acc, chr)                => acc :+ chr
  }
  def prefixFormError(e: FormError): FormError =
    FormError(s"invalid-${camelToKebab(e.key)}", e.message)

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
            err => Left(err.errors.map(prefixFormError)),
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
            err => Left(err.errors.map(prefixFormError)),
            form => Right((LTRuleXMLForm.toLTRuleXML _).tupled(form))
          )
      case r: DbRuleLive if r.ruleType == RuleType.languageToolCore =>
        LTRuleCoreForm.form
          .fillAndValidate(
            r.externalId.getOrElse("")
          )
          .fold(
            err => Left(err.errors.map(prefixFormError)),
            form => Right(LTRuleCoreForm.toLTRuleCore(form))
          )
      case r: DbRuleLive if r.ruleType == RuleType.dictionary =>
        DictionaryForm.form
          .fillAndValidate(
            r.pattern.getOrElse(""),
            r.category.getOrElse(""),
            r.externalId.getOrElse("")
          )
          .fold(
            err => Left(err.errors),
            form => Right((DictionaryForm.toDictionary _).tupled(form))
          )
      case other =>
        Left(
          Seq(
            FormError(
              s"db-to-checker-rule",
              s"Rule type not recognised for DBRule $other"
            )
          )
        )
    }
  }

  def getDraftRules()(implicit session: DBSession = autoSession): List[DbRuleDraft] =
    DbRuleDraft.findAll()

  def searchDraftRules(page: Int, queryStr: Option[String])(implicit
      session: DBSession = autoSession
  ): PaginatedResponse[DbRuleDraft] =
    DbRuleDraft.searchRules(page, queryStr)

  def getDraftDictionaryRules(word: Option[String], page: Int)(implicit
      session: DBSession = autoSession
  ): PaginatedResponse[DbRuleDraft] =
    DbRuleDraft.searchRules(page, word)

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
        .map(e => Seq(FormError("write-live-table", e.getMessage)))

      _ <- publishLiveRules(bucketRuleResource)
      allRuleData <- getAllRuleData(id)
        .toRight(Seq(FormError("read-live-table", "Rule not found")))
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
              "find-draft-rule",
              s"Could not find a draft rule with id $id"
            )
          )
        )
      _ <- draftRule match {
        case _ if draftRule.isArchived =>
          Left(
            Seq(
              FormError(
                "publish-archived-rule",
                "Only unarchived rules can be published"
              )
            )
          )
        case _ => Right(())
      }
      liveRule = draftRule.toLive(reason)
      externalId <- liveRule.externalId.toRight(Seq(FormError("External id", "required")))
      _ <- liveDbRuleToCheckerRule(liveRule)
      _ <- DbRuleLive
        .findRevision(externalId, draftRule.revisionId)
        .map(_ =>
          Seq(
            FormError(
              "rule-already-exists",
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
        Seq(FormError("write-artefact-to-bucket", l.toString))
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
    RuleTagDraft.destroyAll()
    RuleTagLive.destroyAll()
    DbRuleDraft.destroyAll()
    DbRuleLive.destroyAll()

    incomingRules
      .grouped(100)
      .foreach(group => DbRuleDraft.batchInsert(group))

    val draftRules = DbRuleDraft.findAll()

    val liveRules = draftRules
      .filterNot(_.ignore)
      .map(_.toLive("Imported from Google Sheet").copy(isActive = true))

    liveRules
      .grouped(100)
      .foreach(DbRuleLive.batchInsert(_))

    val persistedRules = getDraftRules()

    val persistedRulesToCompare =
      persistedRules.map(rule => rule.copy(id = None, tags = rule.tags.sorted))
    val incomingRulesToCompare =
      incomingRules.map(rule =>
        rule.copy(id = None, isPublished = !rule.ignore, tags = rule.tags.sorted)
      )

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
            "publish-rules",
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
  ): Either[Exception, Option[AllRuleData]] = {
    try {
      val maybeAllRuleData = for {
        draftRule <- DbRuleDraft.find(id)
        externalId <- draftRule.externalId
        liveRule <- DbRuleLive.findLatestRevision(externalId)
        if liveRule.isActive
        _ <- DbRuleLive.setInactive(externalId, user)
        _ <- publishLiveRules(bucketRuleResource).toOption
        // The draft rule needs to be updated to reflect the fact that it is no longer published
        // This also allows us to republish without editing the rule
        _ <- DbRuleDraft.save(draftRule, user).toOption
        allRuleData <- getAllRuleData(id)
      } yield allRuleData

      Right(maybeAllRuleData)
    } catch {
      case e: Exception => Left(e)
    }
  }
  def archiveRule(
      id: Int,
      user: String
  ): Either[Exception, Option[AllRuleData]] = {
    try {
      val maybeAllRuleData = for {
        draftRule <- DbRuleDraft.find(id)
        if !draftRule.isArchived
        archivedDraftRule = draftRule.copy(isArchived = true)
        _ <- DbRuleDraft.save(archivedDraftRule, user).toOption
        allRuleData <- getAllRuleData(id)
      } yield allRuleData

      Right(maybeAllRuleData)
    } catch {
      case e: Exception => Left(e)
    }
  }

  def unarchiveRule(
      id: Int,
      user: String
  ): Either[Exception, Option[AllRuleData]] = {
    try {
      val maybeAllRuleData = for {
        draftRule <- DbRuleDraft.find(id)
        if draftRule.isArchived
        archivedDraftRule = draftRule.copy(isArchived = false)
        _ <- DbRuleDraft.save(archivedDraftRule, user).toOption
        allRuleData <- getAllRuleData(id)
      } yield allRuleData

      Right(maybeAllRuleData)
    } catch {
      case e: Exception => Left(e)
    }
  }

  def destructivelyPublishDictionaryRules(
      words: List[String],
      bucketRuleResource: BucketRuleResource
  ) = {
    // Destroy existing draft dictionary rules
    DbRuleDraft.destroyDictionaryRules()
    // Destroy existing live dictionary rules
    DbRuleLive.destroyDictionaryRules()

    val initialRuleOrder = DbRuleDraft.getLatestRuleOrder() + 1
    val dictionaryRules = words.zipWithIndex.map(wordAndIndex =>
      DbRuleDraft.withUser(
        id = None,
        ruleType = "dictionary",
        pattern = Some(wordAndIndex._1),
        category = Some("Collins"),
        ignore = false,
        user = "Collins Dictionary",
        ruleOrder = initialRuleOrder + wordAndIndex._2,
        externalId = None
      )
    )

    dictionaryRules
      .grouped(100)
      .foreach(group => DbRuleDraft.batchInsert(group, true))

    val liveRules = DbRuleDraft
      .findAllDictionaryRules()
      .map(_.toLive("From Collins Dictionary", true))

    liveRules
      .grouped(100)
      .foreach(DbRuleLive.batchInsert(_))

    publishLiveRules(bucketRuleResource)
  }
}
