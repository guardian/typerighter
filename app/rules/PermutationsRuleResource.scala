package rules

import model.{Category, Rule, PatternToken}

/**
  * A resource that returns a large number of rules to test throughput.
  */
object PermutationsRuleResource {
  def getRules(): List[Rule] = {
    (1 to 8).permutations.toList.map(list => {
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
  }
}
