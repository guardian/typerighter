package matchers

import model._
import org.scalatest._
import services.MatcherRequest

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

  def getMatch(text: String, fromPos: Int, toPos: Int, matchText: String) = RuleMatch(
    rule = exampleRule,
    fromPos = fromPos,
    toPos = toPos,
    matchedText = text,
    message = "An example rule",
    shortMessage = Some("An example rule"),
    suggestions = List(TextSuggestion("other text")),
    matchContext = matchText
  )


  "check" should "report single matches in short text" in {
    val sampleText = "example text is here"
    val matchText = "example [text] is here"

    val eventuallyMatches = regexValidator.check(
      MatcherRequest(getBlocks(sampleText), "example-category")
    )
    eventuallyMatches.map { matches =>
      matches shouldEqual List(
        getMatch("text", 8, 12, matchText)
      )
    }
  }

  "check" should "report single matches in long text" in {
    val sampleText = """
                       | 123456789 123456789 123456789
                       | 123456789 123456789 123456789
                       | 123456789 123456789 123456789
                       | 123456789 123456789 123456789
                       | text
                       | 123456789 123456789 123456789
                       | 123456789 123456789 123456789
                       | 123456789 123456789 123456789
                       | 123456789 123456789 123456789
                       |""".stripMargin.replace("\n", "")

    val matchText = """
                       |123456789
                       | 123456789 123456789 123456789
                       | 123456789 123456789 123456789
                       | 123456789 123456789 123456789
                       | [text]
                       | 123456789 123456789 123456789
                       | 123456789 123456789 123456789
                       | 123456789 123456789 123456789
                       | 123456789
                       |""".stripMargin.replace("\n", "")

    val eventuallyMatches = regexValidator.check(
      MatcherRequest(getBlocks(sampleText), "example-category")
    )
    eventuallyMatches.map { matches =>
      matches shouldEqual List(
        getMatch("text", 121, 125, matchText)
      )
    }
  }

  "check" should "report multiple matches" in {
    val eventuallyMatches = regexValidator.check(
      MatcherRequest(getBlocks("text text text"), "example-category")
    )
    eventuallyMatches.map { matches =>
      matches shouldEqual List(
        getMatch("text", 0, 4, "[text] text text"),
        getMatch("text", 5, 9, "text [text] text"),
        getMatch("text", 10, 14, "text text [text]")
      )
    }
  }
}
