package com.gu.typerighter.fixtures

import com.gu.typerighter.model.{Category, ComparableRegex, RegexRule, RuleMatch}

object RuleMatchFixtures {
  def getRuleMatch(from: Int, to: Int) = RuleMatch(
    rule = RegexRule(
      id = "test-rule",
      description = "test-description",
      category = Category("test-category", "Test Category"),
      regex = new ComparableRegex("test")
    ),
    fromPos = from,
    toPos = to,
    precedingText = "",
    subsequentText = "",
    matchedText = "placeholder text",
    message = "placeholder message",
    matchContext = "[placeholder text]"
  )
}
