package services

import model.{Category, PatternRule, PatternToken}

/**
  * A resource that returns a large number of rules to test throughput.
  */
object PermutationsRuleResource {
  def getRules(): List[PatternRule] = {
    (1 to 8).permutations.toList.map(list => {
      val id = list.mkString
      PatternRule(
        id,
        category = Category("PERMS", "Test permutations"),
        languageShortcode = "en",
        patternTokens = Some(List(PatternToken(
          token = list.mkString,
          caseSensitive = false,
          regexp = true,
          inflected = false
        ))),
        description = s"A test pattern for the string '$id'",
        message = "",
        url = None
      )
    })
  }
}
