package db

import com.gu.typerighter.model.{
  Category,
  CheckerRuleResource,
  ComparableRegex,
  LTRuleCore,
  LTRuleXML,
  RegexRule
}
import com.softwaremill.diffx.generic.auto.diffForCaseClass
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffShouldMatcher._
import scalikejdbc.scalatest.AutoRollback
import service.DbRuleManager

import scala.util.Random

class DbRuleManagerSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {

  def createRandomRules(ruleCount: Int, ignore: Boolean = false) =
    (1 to ruleCount).map { ruleIndex =>
      DbRuleDraft.withUser(
        id = None,
        category = Some("Check this"),
        description = Some("A random rule description. " * Random.between(0, 100)),
        replacement = None,
        pattern = Some(
          s"\b(${Random.shuffle(List("some", "random", "things", "to", "match", "on")).mkString("|")}) by"
        ),
        ignore = ignore,
        notes = Some(s"\b(${Random.shuffle(List("some", "random", "notes", "to", "test"))})"),
        googleSheetId = Some(s"rule-at-index-${ruleIndex}"),
        forceRedRule = Some(true),
        advisoryRule = Some(true),
        user = "Google Sheet",
        ruleType = "regex"
      )
    }.toList

  behavior of "DbRuleManager"

  "destructivelyDumpRuleResourceToDB" should "add rules of each type in a ruleResource, and read it back as an identical resource" in {
    () =>
      val rulesFromSheet = List(
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

      val rules = rulesFromSheet.map(DbRuleManager.checkerRuleToDraftDbRule)
      val rulesFromDb =
        DbRuleManager.destructivelyDumpRulesToDB(rules).map(_.map(_.copy(id = None)))

      rulesFromDb.shouldEqual(Right(rules))
  }

  "destructivelyDumpRulesToDB" should "add 1000 randomly generated rules in a ruleResource, and read them back from the DB as an identical resource" in {
    () =>
      val rules = createRandomRules(1000)
      val rulesFromDb =
        DbRuleManager.destructivelyDumpRulesToDB(rules).map(_.map(_.copy(id = None)))

      rulesFromDb.shouldEqual(Right(rules))
  }

  "destructivelyDumpRulesToDB" should "remove old rules before adding new ones" in { () =>
    val firstRules = createRandomRules(10)
    DbRuleManager.destructivelyDumpRulesToDB(firstRules)

    val secondRules = createRandomRules(10)
    val secondRulesFromDb =
      DbRuleManager.destructivelyDumpRulesToDB(secondRules).map(_.map(_.copy(id = None)))

    secondRulesFromDb.shouldEqual(Right(secondRules))
  }

  "createRuleResourceFromDbRules" should "not translate dbRules into RuleResource if ignore is true" in {
    () =>
      val rulesToIgnore = createRandomRules(10, ignore = true)
      val ruleResourceWithIgnoredRules =
        DbRuleManager.createCheckerRuleResourceFromDbRules(rulesToIgnore)

      ruleResourceWithIgnoredRules.shouldEqual(Right(CheckerRuleResource(List())))
  }

  "destructivelyDumpRuleResourceToDB" should "write all rules to draft, and only write unignored rules to live" in {
    implicit session =>
      val allRules = createRandomRules(2).zipWithIndex.map { case (rule, index) =>
        if (index % 2 == 0) rule.copy(ignore = true) else rule
      }
      val unignoredRules = allRules.filterNot(_.ignore)

      DbRuleManager.destructivelyDumpRulesToDB(allRules)

      DbRuleDraft.findAll().map(_.copy(id = None)) shouldMatchTo allRules
      DbRuleLive.findAll().map(_.copy(id = None)) shouldMatchTo unignoredRules.map(
        _.toLive("Imported from Google Sheet")
      )
  }
}
