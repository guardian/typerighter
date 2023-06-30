package fixtures

import db.DbRuleDraft

import scala.util.Random

object Rules {
  def createRandomRules(ruleCount: Int, ignore: Boolean = false): List[DbRuleDraft] =
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
}
