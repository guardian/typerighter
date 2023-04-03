package db

import com.gu.typerighter.model.{Category, ComparableRegex, LTRuleCore, LTRuleXML, RegexRule, RuleResource}
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback
import service.DbRuleManager

import scala.util.Random

class DbRuleManagerSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {

  def createRandomRules(ruleCount: Int) =
    (1 to ruleCount).map { ruleIndex =>
      RegexRule(
        s"rule-at-index-${ruleIndex}",
        Category("Check this", "Check this"),
        "A random rule description. " * Random.between(0, 100),
        List(),
        None,
        new ComparableRegex(
          s"\b(${Random.shuffle(List("some", "random", "things", "to", "match", "on")).mkString("|")}) by"
        )
      )
    }.toList

  behavior of "DbRuleManager"

  "destructivelyDumpRuleResourceToDB" should "add rules of each type in a ruleResource, and read it back as an identical resource" in {
    implicit session =>
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

      val rules = rulesFromSheet.map(DbRuleManager.baseRuleToDbRule)
      val rulesFromDb = DbRuleManager.destructivelyDumpRuleResourceToDB(rules)

      rulesFromDb.shouldEqual(Right(rules))
  }

  "destructivelyDumpRuleResourceToDB" should "add 1000 randomly generated rules in a ruleResource, and read them back from the DB as an identical resource" in {
    implicit session =>
      val rulesFromSheet = createRandomRules(1000)

      val rules = rulesFromSheet.map(DbRuleManager.baseRuleToDbRule)
      val rulesFromDb = DbRuleManager.destructivelyDumpRuleResourceToDB(rules)

      rulesFromDb.shouldEqual(Right(rules))
  }

  "destructivelyDumpRuleResourceToDB" should "remove old rules before adding new ones" in {
    implicit session =>
      val firstRules = createRandomRules(10).map(DbRuleManager.baseRuleToDbRule)
      DbRuleManager.destructivelyDumpRuleResourceToDB(firstRules)

      val secondRules = createRandomRules(10)
      val secondRulesFromDb =
        DbRuleManager.destructivelyDumpRuleResourceToDB(secondRules.map(DbRuleManager.baseRuleToDbRule))

      secondRulesFromDb.shouldEqual(Right(RuleResource(secondRules)))
  }
}
