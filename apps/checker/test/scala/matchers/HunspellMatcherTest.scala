package matchers

import model._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffMatcher.{matchTo, _}
import services.MatcherRequest

class HunspellMatcherTest extends AsyncFlatSpec with Matchers {
  val category = Category("example-category", "Example category")
  val hunspellMatcher = new HunspellMatcher(category, "conf/resources/hunspell/example")

  def getBlocks(text: String, from: Int = 0) = List(TextBlock("text-block-id", text, from, from + text.length))

  "check" should "not match for known words" in {
    val eventuallyMatches = hunspellMatcher.check(
      MatcherRequest(getBlocks("Example text with John Smith"))
    )
    eventuallyMatches.map { matches =>
      matches shouldEqual Nil
    }
  }

  "check" should "fuzzy match known words with suggestions" in {
    val text = "Example text with John Simth"
    val eventuallyMatches = hunspellMatcher.check(
      MatcherRequest(getBlocks(text))
    )
    eventuallyMatches.map { matches =>
      matches should matchTo(List(
        hunspellMatcher.getRuleMatch("Simth", 23, 28, List(TextSuggestion("Smith")), text),
      ))
    }
  }

  "check" should "produce correct ranges for blocks" in {
    val text = "Jerry Brzeczek"
    val eventuallyMatches = hunspellMatcher.check(
      MatcherRequest(getBlocks(text, 2))
    )
    eventuallyMatches.map { matches =>
      matches should matchTo(List(
        hunspellMatcher.getRuleMatch("Jerry", 2, 7, List(TextSuggestion("Jerzy")), text),
        hunspellMatcher.getRuleMatch("Brzeczek", 8, 16, List(TextSuggestion("Brzęczek")), text),
      ))
    }
  }

  "check" should "report multiple matches" in {
    val text = "Example unknown with John Simth"
    val eventuallyMatches = hunspellMatcher.check(
      MatcherRequest(getBlocks(text))
    )
    eventuallyMatches.map { matches =>
      matches should matchTo(List(
        hunspellMatcher.getRuleMatch("unknown", 8, 15, Nil, text),
        hunspellMatcher.getRuleMatch("Simth", 26, 31, List(TextSuggestion("Smith")), text),
      ))
    }
  }

  "check" should "correctly handle pluralisation" in {
    val text = "Example word with John Smiths"
    val eventuallyMatches = hunspellMatcher.check(
      MatcherRequest(getBlocks(text))
    )
    eventuallyMatches.map { matches =>
      matches should matchTo(List(
        hunspellMatcher.getRuleMatch("Smiths", 23, 29, List(
          TextSuggestion("Smith"),
          TextSuggestion("Smith's")), text),
      ))
    }
  }

  "check" should "report unknown words" in {
    val text = "Example text with unknown word"
    val eventuallyMatches = hunspellMatcher.check(
      MatcherRequest(getBlocks(text))
    )
    eventuallyMatches.map { matches =>
      matches should matchTo(List(
        hunspellMatcher.getRuleMatch("unknown", 18, 25, Nil, text)
      ))
    }
  }
}
