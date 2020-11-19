package model

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class BaseRuleTest extends AsyncFlatSpec with Matchers {
  behavior of "RegexRule"

  it should "transform suggestions if the regex in question is not case sensitive, preserving case when suggestions match case-insensitively" in {
    val suggestion = TextSuggestion("medieval")
    val rule = RegexRule(
      id = "test-rule",
      description = "test-description",
      category = Category("test-category", "Test Category"),
      regex = "(?i)\\bmedia?eval"r,
      suggestions = List(suggestion),
      replacement = Some(suggestion)
    )

    val ruleMatch = rule.toMatch(0, 8, TextBlock("id", "Medieval", 0, 8))

    ruleMatch.suggestions shouldBe List(TextSuggestion("Medieval"))
  }

  it should "transform suggestions if the regex in question is not case sensitive, preserving case when suggestions do not match, but the first character matches case-insensitively, to work around sentence starts" in {
    val suggestion = TextSuggestion("medieval")
    val rule = RegexRule(
      id = "test-rule",
      description = "test-description",
      category = Category("test-category", "Test Category"),
      regex = "(?i)\\bmedia?eval"r,
      suggestions = List(suggestion),
      replacement = Some(suggestion)
    )

    val ruleMatch = rule.toMatch(0, 8, TextBlock("id", "Mediaval", 0, 8))

    ruleMatch.suggestions shouldBe List(TextSuggestion("Medieval"))
  }

  it should "not transform suggestions if the regex in question is case sensitive" in {
    val suggestion = TextSuggestion("medieval")
    val rule = RegexRule(
      id = "test-rule",
      description = "test-description",
      category = Category("test-category", "Test Category"),
      regex = "\\bmedia?eval"r,
      suggestions = List(suggestion),
      replacement = Some(suggestion)
    )

    val ruleMatch = rule.toMatch(0, 7, TextBlock("id", "Medieval", 0, 8))

    ruleMatch.suggestions shouldBe List(TextSuggestion("medieval"))
  }
}
