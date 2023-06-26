package service

import com.gu.typerighter.model.{
  Category,
  CheckerRule,
  CheckerRuleResource,
  ComparableRegex,
  LTRuleCore,
  LTRuleXML,
  RegexRule,
  TextSuggestion
}
import com.gu.typerighter.rules.BucketRuleResource
import db.{DBTest, DbRuleDraft, DbRuleLive}
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback
import com.softwaremill.diffx.generic.auto.diffForCaseClass
import com.softwaremill.diffx.scalatest.DiffShouldMatcher._
import play.api.data.FormError
import utils.LocalStack

import scala.util.Random

class RuleManagerSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {
  val bucketRuleResource =
    new BucketRuleResource(LocalStack.s3Client, "typerighter-app-local", "local")

  def toCheckerRules(rules: List[DbRuleDraft]) =
    rules
      .map(_.toLive("Imported from Google Sheet"))
      .map(RuleManager.liveDbRuleToCheckerRule(_).toOption.get)

  def createRandomRules(ruleCount: Int, ignore: Boolean = false) =
    (1 to ruleCount).map { ruleIndex =>
      DbRuleDraft.withUser(
        id = Some(ruleIndex),
        category = Some("Check this"),
        description = Some("A random rule description. " * Random.between(1, 100)),
        replacement = None,
        pattern = Some(
          s"\b(${Random.shuffle(List("some", "random", "things", "to", "match", "on")).mkString("|")}) by"
        ),
        ignore = ignore,
        notes = Some(s"\b(${Random.shuffle(List("some", "random", "notes", "to", "test"))})"),
        externalId = Some(s"rule-at-index-$ruleIndex"),
        forceRedRule = Some(true),
        advisoryRule = Some(true),
        user = "Google Sheet",
        ruleType = "regex"
      )
    }.toList

  behavior of "DbRuleManager"

  "liveDbRuleToCheckerRule" should "create a checker rule from a live rule" in { () =>
    val rule = createRandomRules(1).head.toLive("reason")
    val maybeCheckerRule = RuleManager.liveDbRuleToCheckerRule(rule)

    maybeCheckerRule shouldBe Right(
      RegexRule(
        id = rule.externalId.get,
        regex = new ComparableRegex(rule.pattern.get),
        description = rule.description.get,
        replacement = rule.replacement.map(r => TextSuggestion(r)),
        category = Category(rule.category.get, rule.category.get)
      )
    )
  }

  "liveDbRuleToCheckerRule" should "give a sensible error message when parsing regex rules" in {
    () => () =>
      val rule = DbRuleDraft
        .withUser(id = None, ruleType = "regex", user = "example.user", ignore = false)
        .toLive("reason")

      val maybeCheckerRule = RuleManager.liveDbRuleToCheckerRule(rule)

      maybeCheckerRule shouldBe Left(
        List(
          FormError("pattern", List("error.required"), List()),
          FormError("category", List("error.required"), List()),
          FormError("description", List("error.required"), List()),
          FormError("externalId", List("error.required"), List())
        )
      )
  }

  "liveDbRuleToCheckerRule" should "give a sensible error message when parsing ltXML rules" in {
    () =>
      val rule = DbRuleDraft
        .withUser(id = Some(0), ruleType = "languageToolXML", user = "example.user", ignore = false)
        .toLive("reason")

      val maybeCheckerRule = RuleManager.liveDbRuleToCheckerRule(rule)

      maybeCheckerRule shouldBe Left(
        List(
          FormError("pattern", List("error.required"), List()),
          FormError("category", List("error.required"), List()),
          FormError("description", List("error.required"), List()),
          FormError("externalId", List("error.required"), List())
        )
      )
  }

  "liveDbRuleToCheckerRule" should "give a sensible error message when parsing ltCore rules" in {
    () =>
      val rule = DbRuleDraft
        .withUser(
          id = Some(0),
          ruleType = "languageToolCore",
          user = "example.user",
          ignore = false
        )
        .toLive("reason")

      val maybeCheckerRule = RuleManager.liveDbRuleToCheckerRule(rule)

      maybeCheckerRule shouldBe Left(
        List(
          FormError("externalId", List("error.required"), List())
        )
      )
  }

  "destructivelyPublishRules" should "add rules of each type in a ruleResource, and read it back as an identical resource" in {
    () =>
      val rulesFromSheet = List[CheckerRule](
        RegexRule(
          "faef1f8a-4ee2-4b97-8783-0566e27851da",
          Category("Check this", "Check this"),
          "**bearing children** Such phrases as “she bore him two sons” and “he had two children by” are outdated and sexist",
          List(),
          None,
          new ComparableRegex("\b(children|sons?|daughters?) by")
        ),
        LTRuleXML(
          "DATEFORMAT",
          "<rulegroup></rulegroup>",
          Category("Check this", "Check this"),
          "An example description"
        ),
        LTRuleCore(
          "DOUBLE_PUNCTUATION",
          "DOUBLE_PUNCTUATION"
        )
      )

      val rules = rulesFromSheet
        .map(RuleManager.checkerRuleToDraftDbRule)
        .zipWithIndex
        .map { case (rule, index) => rule.copy(id = Some(index + 1)) }

      val maybePublishedRules =
        RuleManager.destructivelyPublishRules(rules, bucketRuleResource)

      maybePublishedRules.toOption.get.rules shouldMatchTo rulesFromSheet
  }

  "destructivelyDumpRulesToDB" should "add 1000 randomly generated rules in a ruleResource, and read them back from the DB as an identical resource" in {
    () =>
      val rules = createRandomRules(1)
      val rulesAsPublished = rules
        .map(_.toLive("Imported from Google Sheet"))
        .map(RuleManager.liveDbRuleToCheckerRule(_).toOption.get)

      val rulesFromDb =
        RuleManager.destructivelyPublishRules(rules, bucketRuleResource).toOption.get

      rulesFromDb.rules shouldMatchTo rulesAsPublished
  }

  "destructivelyDumpRulesToDB" should "remove old rules before adding new ones" in { () =>
    val firstRules = createRandomRules(10)
    RuleManager.destructivelyPublishRules(firstRules, bucketRuleResource)

    val secondRules = createRandomRules(10)
    val secondRulesFromDb =
      RuleManager.destructivelyPublishRules(secondRules, bucketRuleResource).toOption.get

    secondRulesFromDb.rules shouldMatchTo toCheckerRules(secondRules)
  }

  "destructivelyDumpRulesToDB" should "make newly added rules active" in { () =>
    val firstRules = createRandomRules(1)
    RuleManager.destructivelyPublishRules(firstRules, bucketRuleResource)

    DbRuleLive.findAll().head.isActive shouldBe true
  }

  "createRuleResourceFromDbRules" should "not translate dbRules into RuleResource if ignore is true" in {
    () =>
      val rulesToIgnore = createRandomRules(10, ignore = true)

      val ruleResourceWithIgnoredRules =
        RuleManager.destructivelyPublishRules(rulesToIgnore, bucketRuleResource)

      ruleResourceWithIgnoredRules.toOption.get.shouldEqual(CheckerRuleResource(List()))
  }

  "destructivelyPublishRules" should "write all rules to draft, and only write unignored rules to live" in {
    () =>
      val allRules = createRandomRules(2).zipWithIndex.map { case (rule, index) =>
        if (index % 2 == 0) rule.copy(ignore = true) else rule
      }
      val unignoredRules = allRules.filterNot(_.ignore)

      RuleManager.destructivelyPublishRules(allRules, bucketRuleResource)

      DbRuleDraft.findAll() shouldMatchTo allRules.map(rule =>
        rule.copy(isPublished = !rule.ignore)
      )
      DbRuleLive.findAll() shouldMatchTo unignoredRules.map(
        _.toLive("Imported from Google Sheet").copy(isActive = true)
      )
  }

  val user = "example.user@guardian.co.uk"
  val reason = "Some important update"

  def createPublishableRule =
    DbRuleDraft
      .create(
        ruleType = "regex",
        user = user,
        ignore = false,
        pattern = Some("pattern"),
        description = Some("description"),
        category = Some("category")
      )
      .get

  "publishRule" should "add an identical rule to the live rules table, and make it active" in {
    () =>
      val ruleToPublish = createPublishableRule

      RuleManager.publishRule(ruleToPublish.id.get, user, reason, bucketRuleResource).toOption.get

      DbRuleLive.findRevision(ruleToPublish.externalId.get, ruleToPublish.revisionId) match {
        case Some(liveRule) =>
          liveRule.ruleType shouldBe ruleToPublish.ruleType
          liveRule.pattern shouldBe ruleToPublish.pattern
          liveRule.description shouldBe ruleToPublish.description
          liveRule.reason shouldBe reason
          liveRule.isActive shouldBe true
        case None => fail(s"Could not find published rule with id: ${ruleToPublish.id.get}")
      }
  }

  "publishRule" should "not publish rules that cannot be transformed into checker rules" in { () =>
    val user = "example.user@guardian.co.uk"
    val reason = "Some important update"
    val ruleToPublish = DbRuleDraft.create(ruleType = "regex", user = user, ignore = false).get

    RuleManager.publishRule(ruleToPublish.id.get, user, reason, bucketRuleResource) match {
      case Right(_)         => fail("This rule should not be publishable")
      case Left(formErrors) => formErrors.head.message should include("error.required")
    }
  }

  "publishRule" should "make previous revisions of that rule inactive" in { () =>
    val ruleToPublish = createPublishableRule

    RuleManager.publishRule(ruleToPublish.id.get, user, reason, bucketRuleResource)

    DbRuleDraft.save(ruleToPublish.copy(revisionId = 2), user)

    RuleManager.publishRule(ruleToPublish.id.get, user, reason, bucketRuleResource)

    val allLiveRules = DbRuleLive.findAll()
    allLiveRules(0).isActive shouldBe false
    allLiveRules(1).isActive shouldBe true
  }

  "publishRule" should "not alter the active state of other rules when publishing a rule" in { () =>
    val ruleToPublish = createPublishableRule
    val anotherRuleToPublish = createPublishableRule

    RuleManager.publishRule(ruleToPublish.id.get, user, reason, bucketRuleResource)
    RuleManager.publishRule(anotherRuleToPublish.id.get, user, reason, bucketRuleResource)

    val allLiveRules = DbRuleLive.findAll()
    allLiveRules(0).isActive shouldBe true
    allLiveRules(1).isActive shouldBe true
  }

  "publishRule" should "should not be able to publish the same revision of a rule" in { () =>
    val ruleToPublish = createPublishableRule

    RuleManager.publishRule(ruleToPublish.id.get, user, reason, bucketRuleResource)
    RuleManager.publishRule(ruleToPublish.id.get, user, reason, bucketRuleResource) match {
      case Right(_) => fail("This rule should not be publishable")
      case Left(formErrors) =>
        formErrors.head.message should include(
          "has not changed"
        )
    }
  }

  "parseDraftRuleForPublication" should "should give validation errors when the rule is incomplete" in {
    () =>
      val ruleToPublish = DbRuleDraft
        .create(
          ruleType = "regex",
          user = user,
          ignore = false
        )
        .get
      RuleManager.parseDraftRuleForPublication(ruleToPublish.id.get, "reason") match {
        case Right(_)         => fail("This rule should not be publishable")
        case Left(formErrors) => formErrors.length shouldBe 3
      }
  }

  "parseDraftRuleForPublication" should "should give validation errors when a live rule of that revision id already exists" in {
    () =>
      val ruleToPublish = createPublishableRule
      RuleManager.publishRule(ruleToPublish.id.get, user, reason, bucketRuleResource)
      RuleManager.parseDraftRuleForPublication(ruleToPublish.id.get, "reason") match {
        case Right(_) => fail("This rule should not be publishable")
        case Left(formErrors) =>
          formErrors.length shouldBe 1
          formErrors.head.message should include("has not changed")
      }
  }

  "getAllRuleData" should "return the current draft rule" in { () =>
    val ruleToPublish = createPublishableRule

    RuleManager.getAllRuleData(ruleToPublish.id.get) match {
      case None => fail("Rule should exist")
      case Some(allRuleData) =>
        allRuleData.draft shouldMatchTo ruleToPublish
        allRuleData.live shouldMatchTo List.empty
    }
  }

  "getAllRuleData" should "return the current draft rule, the live rule if it exists, and the publication history" in {
    () =>
      val ruleToPublish = createPublishableRule

      val firstLiveRule =
        RuleManager.publishRule(ruleToPublish.id.get, user, reason, bucketRuleResource).toOption.get
      val revisedRuleToPublish = DbRuleDraft.save(ruleToPublish.copy(revisionId = 2), user).get
      val secondLiveRule =
        RuleManager.publishRule(ruleToPublish.id.get, user, reason, bucketRuleResource).toOption.get

      secondLiveRule.draft shouldMatchTo revisedRuleToPublish
      secondLiveRule.live shouldMatchTo List(
        secondLiveRule.live(0),
        firstLiveRule.live(0).copy(isActive = false)
      )
  }

  "archiveRule" should "do nothing if the rule does not exist in draft" in { () =>
    val invalidId = 100

    RuleManager.archiveRule(invalidId, user) match {
      case Left(e) =>
        fail(s"Unexpected error on archiving id: $invalidId: ${e.getMessage}")
      case Right(maybeDraftRule) =>
        maybeDraftRule shouldBe None
    }
  }

  "archiveRule" should "do nothing if the rule is already archived" in { () =>
    val ruleToArchive = createPublishableRule

    RuleManager.archiveRule(ruleToArchive.id.get, user) match {
      case Left(e) =>
        fail(s"Unexpected error on archiving id: $ruleToArchive.id.get: ${e.getMessage}")
      case Right(maybeDraftRule) =>
        maybeDraftRule shouldBe None
    }
  }

  "archiveRule" should "archive the rule if it (a) exists in draft and (b) is unarchived" in { () =>
    val ruleToArchive = createPublishableRule

    RuleManager.archiveRule(ruleToArchive.id.get, user) match {
      case Left(e) =>
        fail(s"Unexpected error on archiving id: ${ruleToArchive.id.get}: ${e.getMessage}")
      case Right(maybeDraftRule) =>
        val draftRule = maybeDraftRule.get
        draftRule.revisionId shouldMatchTo (ruleToArchive.revisionId + 1)
        draftRule.isArchived shouldBe true
        draftRule.isPublished shouldBe false
        draftRule.updatedBy shouldBe user
        draftRule.updatedAt shouldBe >(ruleToArchive.updatedAt)
    }
  }

  "unpublishRule" should "should do nothing if the rule does not exist in draft" in { () =>
    val ruleToArchive = createPublishableRule

    RuleManager.unpublishRule(ruleToArchive.id.get, user) match {
      case Left(e) =>
        fail(s"Unexpected error on unpublishing id: $ruleToArchive.id.get: ${e.getMessage}")
      case Right(maybeLiveRule) =>
        maybeLiveRule shouldBe None
    }
  }

  "unpublishRule" should "do nothing if the rule does not exist in draft" in { () =>
    val invalidId = 100

    RuleManager.unpublishRule(invalidId, user) match {
      case Left(e) =>
        fail(s"Unexpected error on unpublishing id: $invalidId: ${e.getMessage}")
      case Right(maybeLiveRule) =>
        maybeLiveRule shouldBe None
    }
  }

  "unpublishRule" should "do nothing if the rule does not exist in live" in { () =>
    val ruleToArchive = createPublishableRule

    RuleManager.unpublishRule(ruleToArchive.id.get, user) match {
      case Left(e) =>
        fail(s"Unexpected error on unpublishing id: $ruleToArchive.id.get: ${e.getMessage}")
      case Right(maybeLiveRule) =>
        maybeLiveRule shouldBe None
    }
  }

  "unpublishRule" should "do nothing to the live rule if it is already inactive" in { () =>
    val ruleToUnpublish = createPublishableRule

    RuleManager.publishRule(ruleToUnpublish.id.get, user, reason, bucketRuleResource)

    for {
      liveRule <- DbRuleLive.findRevision(
        ruleToUnpublish.externalId.get,
        ruleToUnpublish.revisionId
      )
      externalId <- liveRule.externalId
      _ <- DbRuleLive.setInactive(externalId, user)
    }
      RuleManager.unpublishRule(ruleToUnpublish.id.get, user) match {
        case Left(e) =>
          fail(s"Unexpected error on unpublishing id: ${ruleToUnpublish.id.get}: ${e.getMessage}")
        case Right(maybeLiveRule) =>
          maybeLiveRule shouldBe None
      }
  }

  "unpublishRule" should "make the rule inactive if it (a) exists in live, and (b) is active" in {
    () =>
      val ruleToUnpublish = createPublishableRule

      RuleManager.publishRule(ruleToUnpublish.id.get, user, reason, bucketRuleResource)

      RuleManager.unpublishRule(ruleToUnpublish.id.get, user) match {
        case Left(e) =>
          fail(s"Unexpected error on unpublishing id: ${ruleToUnpublish.id.get}: ${e.getMessage}")
        case Right(maybeLiveRule) =>
          val liveRule = maybeLiveRule.get
          liveRule.isActive shouldBe false
          liveRule.updatedBy shouldBe user
          liveRule.updatedAt shouldBe >(ruleToUnpublish.updatedAt)
      }
  }
}
