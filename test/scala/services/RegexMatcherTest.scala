package services

import model.{Category, RegexRule, RuleMatch, TextBlock, TextSuggestion}
import org.scalatest._

class RegexMatcherTest extends AsyncFlatSpec with Matchers {
  val exampleRule = RegexRule(
    id = "example-rule",
    category = Category("new-category", "New Category", "puce"),
    description = "An example rule",
    suggestions = List(TextSuggestion("other text")),
    regex = "text".r
  )
  val regexValidator = new RegexMatcher("example-category", List(exampleRule))

  def getBlocks(text: String) = List(TextBlock("text-block-id", text, 0, text.length))

  def getMatch(text: String, fromPos: Int, toPos: Int) = RuleMatch(
    rule = exampleRule,
    fromPos = fromPos,
    toPos = toPos,
    message = "An example rule",
    shortMessage = Some("An example rule"),
    suggestions = List(TextSuggestion("other text"))
  )

  "check" should "report single matches" in {
    val eventuallyMatches = regexValidator.check(
      MatcherRequest(getBlocks("example text"), "example-category")
    )
    eventuallyMatches.map { matches =>
      matches shouldEqual List(
        getMatch("text", 8, 12)
      )
    }
  }

  "check" should "report multiple matches" in {
    val eventuallyMatches = regexValidator.check(
      MatcherRequest(getBlocks("text text text"), "example-category")
    )
    eventuallyMatches.map { matches =>
      matches shouldEqual List(
        getMatch("text", 0, 4),
        getMatch("text", 5, 9),
        getMatch("text", 10, 14)
      )
    }
  }
}
