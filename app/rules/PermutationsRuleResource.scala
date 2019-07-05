package rules

import model.{Category, Rule, PatternToken}

import scala.concurrent.Future

/**
  * A resource that returns a large number of rules to test throughput.
  */
object PermutationsRuleResource extends RuleResource {
  def fetchRulesByCategory(): Future[(Map[Category, List[Rule]], List[String])]= {
    val rules = (1 to 8).permutations.toList.map(list => {
      val id = list.mkString
      Rule(
        id,
        category = Category("PERMS", "Test permutations", "red"),
        languageShortcode = Some("en"),
        patternTokens = Some(List(PatternToken(
          token = list.mkString,
          caseSensitive = false,
          regexp = true,
          inflected = false
        ))),
        description = s"A test pattern for the string '$id'",
        message = "",
        url = None,
        suggestions = List.empty
      )
    })
    Future.successful((rules.groupBy(_.category), List.empty[String]))
  }
}
