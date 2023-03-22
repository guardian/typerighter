package db

import com.gu.typerighter.model.{BaseRule, Category, ComparableRegex, LTRuleXML, RegexRule, RuleResource}
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffShouldMatcher._
import com.softwaremill.diffx.generic.auto._
import play.api.libs.json.Json
import scalikejdbc.scalatest.AutoRollback
import service.DbRuleManager

import scala.io.Source
import scala.util.Random

class DbRuleManagerSpec
    extends FixtureAnyFlatSpec
    with Matchers
    with AutoRollback
    with DBTest {

  def createRandomRules(ruleCount: Int) =
    (1 to ruleCount).map { ruleIndex =>
      RegexRule(
        s"rule-at-index-${ruleIndex}",
        Category("Check this", "Check this"),
        "A random rule description. " * Random.between(0, 100),
        List(),
        None,
        new ComparableRegex(s"\b(${Random.shuffle(List("some", "random", "things", "to", "match", "on")).mkString("|")}) by")
      )
    }.toList

  behavior of "DbRuleManager"

  "overwriteAllRules" should "add a single rule in a ruleResource, and read it back as an identical resource" in { implicit session =>
    val rulesFromSheet = List(
      RegexRule(
        "faef1f8a-4ee2-4b97-8783-0566e27851da",
        Category("Check this", "Check this"),
        "**bearing children** Such phrases as “she bore him two sons” and “he had two children by” are outdated and sexist",
        List(),
        None,
        new ComparableRegex("\b(children|sons?|daughters?) by")
      )
    )

    val rules = RuleResource(rules = rulesFromSheet, ltDefaultRuleIds = List.empty)
    val rulesFromDb = DbRuleManager.overwriteAllRules(rules)

    rulesFromDb.shouldEqual(rules)
  }

  "overwriteAllRules" should "add 1000 randomly generated rules in a ruleResource, and read them back from the DB as an identical resource" in { implicit session =>
    val rulesFromSheet = createRandomRules(1000)

    val rules = RuleResource(rules = rulesFromSheet, ltDefaultRuleIds = List.empty)
    val rulesFromDb = DbRuleManager.overwriteAllRules(rules)

    rulesFromDb.shouldEqual(rules)
  }
}
