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

class DbRuleManagerSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {
  val bucketRuleResource =
    new BucketRuleResource(LocalStack.s3Client, "typerighter-app-local", "local")

  def toCheckerRules(rules: List[DbRuleDraft]) =
    rules
      .map(_.toLive("Imported from Google Sheet"))
      .map(RuleManager.liveDbRuleToCheckerRule(_).toOption.get)

  def createRandomRules(ruleCount: Int, ignore: Boolean = false) =
    (1 to ruleCount).map { ruleIndex =>
      DbRuleDraft.withUser(
        id = None,
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
        .withUser(id = None, ruleType = "languageToolXML", user = "example.user", ignore = false)
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
        .withUser(id = None, ruleType = "languageToolCore", user = "example.user", ignore = false)
        .toLive("reason")

      val maybeCheckerRule = RuleManager.liveDbRuleToCheckerRule(rule)

      maybeCheckerRule shouldBe Left(
        List(
          FormError("externalId", List("error.required"), List())
        )
      )
  }

  "destructivelyDumpRuleResourceToDB" should "add rules of each type in a ruleResource, and read it back as an identical resource" in {
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

      val rules = rulesFromSheet.map(RuleManager.checkerRuleToDraftDbRule)
      val maybePublishedRules =
        RuleManager.destructivelyPublishRules(rules, bucketRuleResource)

      println(maybePublishedRules)

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

  "createRuleResourceFromDbRules" should "not translate dbRules into RuleResource if ignore is true" in {
    () =>
      val rulesToIgnore = createRandomRules(10, ignore = true)

      val ruleResourceWithIgnoredRules =
        RuleManager.destructivelyPublishRules(rulesToIgnore, bucketRuleResource).toOption.get

      ruleResourceWithIgnoredRules.shouldEqual(CheckerRuleResource(List()))
  }

  "destructivelyDumpRuleResourceToDB" should "write all rules to draft, and only write unignored rules to live" in {
    () =>
      val allRules = createRandomRules(2).zipWithIndex.map { case (rule, index) =>
        if (index % 2 == 0) rule.copy(ignore = true) else rule
      }
      val unignoredRules = allRules.filterNot(_.ignore)

      RuleManager.destructivelyPublishRules(allRules, bucketRuleResource)

      DbRuleDraft.findAll().map(_.copy(id = None)) shouldMatchTo allRules
      DbRuleLive.findAll().map(_.copy(id = None)) shouldMatchTo unignoredRules.map(
        _.toLive("Imported from Google Sheet")
      )
  }

  "publishRule" should "add an identical rule to the live rules table" in { () =>
    val user = "example.user@guardian.co.uk"
    val reason = "Some important update"
    val ruleToPublish = DbRuleDraft
      .create(
        ruleType = "regex",
        user = user,
        ignore = false,
        pattern = Some("pattern"),
        description = Some("description"),
        category = Some("category")
      )
      .get

    val publishedRule =
      RuleManager.publishRule(ruleToPublish.id.get, user, reason, bucketRuleResource).toOption.get

    DbRuleLive.find(publishedRule.id.get) match {
      case Some(liveRule) =>
        liveRule.ruleType shouldBe ruleToPublish.ruleType
        liveRule.pattern shouldBe ruleToPublish.pattern
        liveRule.description shouldBe ruleToPublish.description
        liveRule.reason shouldBe reason
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
}
