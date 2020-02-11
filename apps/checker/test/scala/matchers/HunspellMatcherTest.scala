package matchers

import model._
import org.scalatest._
import services.MatcherRequest

class HunspellMatcherTest extends AsyncFlatSpec with Matchers {
  val hunspellMatcher = new HunspellMatcher("example-category", "conf/resources/hunspell/example")

  def getBlocks(text: String) = List(TextBlock("text-block-id", text, 0, text.length))

  def getMatch(text: String, fromPos: Int, toPos: Int, suggestions: List[String] = Nil) = RuleMatch(
    rule = HunspellMatcher.hunspellRule,
    fromPos = fromPos,
    toPos = toPos,
    matchedText = text,
    message = HunspellMatcher.hunspellMessage,
    shortMessage = Some(HunspellMatcher.hunspellMessage),
    suggestions = suggestions.map(s => TextSuggestion(s))
  )

  "check" should "report single matches" in {
    val eventuallyMatches = hunspellMatcher.check(
      MatcherRequest(getBlocks("example text"), "example-category")
    )
    eventuallyMatches.map { matches =>
      matches shouldEqual List(
        getMatch("text", 8, 12, List("example"))
      )
    }
  }

  "check" should "report multiple matches" in {
    val eventuallyMatches = hunspellMatcher.check(
      MatcherRequest(getBlocks("text text text"), "example-category")
    )
    eventuallyMatches.map { matches =>
      matches shouldEqual List(
        getMatch("text", 0, 4, List("example")),
        getMatch("text", 5, 9, List("example")),
        getMatch("text", 10, 14, List("example"))
      )
    }
  }
}
