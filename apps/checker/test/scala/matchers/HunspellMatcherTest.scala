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

  "check" should "not match for known words" in {
    val eventuallyMatches = hunspellMatcher.check(
      MatcherRequest(getBlocks("Example text with John Smith"), "example-category")
    )
    eventuallyMatches.map { matches =>
      matches shouldEqual Nil
    }
  }

  "check" should "fuzzy match known words with suggestions" in {
    val eventuallyMatches = hunspellMatcher.check(
      MatcherRequest(getBlocks("Example text with John Simth"), "example-category")
    )
    eventuallyMatches.map { matches =>
      matches shouldEqual List(
        getMatch("Simth", 23, 28, List("Smith")),
      )
    }
  }

  "check" should "report multiple matches" in {
    val eventuallyMatches = hunspellMatcher.check(
      MatcherRequest(getBlocks("Example unknown with John Simth"), "example-category")
    )
    eventuallyMatches.map { matches =>
      matches shouldEqual List(
        getMatch("unknown", 8, 15, Nil),
        getMatch("Simth", 26, 31, List("Smith")),
      )
    }
  }

  "check" should "correctly handle pluralisation" in {
    val eventuallyMatches = hunspellMatcher.check(
      MatcherRequest(getBlocks("Example word with John Smiths"), "example-category")
    )
    eventuallyMatches.map { matches =>
      matches shouldEqual List(
        getMatch("Smiths", 23, 29, List(
          // It looks like Hunspell's fuzzy matching is very fuzzy for small dictionaries.
          "Smith",
          "Smith's",
          "With")),
      )
    }
  }

  "check" should "report unknown words" in {
    val eventuallyMatches = hunspellMatcher.check(
      MatcherRequest(getBlocks("Example text with unknown word"), "example-category")
    )
    eventuallyMatches.map { matches =>
      matches shouldEqual List(
        getMatch("unknown", 18, 25, Nil)
      )
    }
  }
}
