package db

import com.gu.typerighter.model.{Category, ComparableRegex, RegexRule, RuleResource}
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback
import service.DbRuleManager

class DbRuleManagerSpec
    extends FixtureAnyFlatSpec
    with Matchers
    with AutoRollback
    with RuleManagerDBTest {

  "DbRuleManager" should "overwrite all rules" in { implicit session =>
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

}
