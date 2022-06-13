package matchers

import model._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffMatcher.{matchTo, _}
import services.MatcherRequest

class DictionaryMatcherTest extends AsyncFlatSpec with Matchers {
  val category = Category("example-category", "Example category")
  val exampleNames = Set(
    "Eva Smith",
    "Daisy Renton",
    "John Smith",
    "Nancy Cartwright",
    "Fiona Lewis",
    "Jerzy Brzęczek",
    "Birling"
  )
  val dictionaryMatcher = new DictionaryMatcher(category, "conf/resources/hunspell/example", exampleNames)

  def getBlocks(text: String, from: Int = 0) = List(TextBlock("text-block-id", text, from, from + text.length))

  behavior of "check"

  it should "not match for known words" in {
    val eventuallyMatches = dictionaryMatcher.check(MatcherRequest(getBlocks("Example text with known words")))

    eventuallyMatches.map { matches =>
      matches shouldEqual Nil
    }
  }

  it should "not match for punctuation" in {
    val eventuallyMatches = dictionaryMatcher.check(MatcherRequest(getBlocks("Example text, with known words.")))

    eventuallyMatches.map { matches =>
      matches shouldEqual Nil
    }
  }

  it should "not match for non-word character sequences that we intentionally ignore" in {
    val nonWordExamples = List(
      "an.example@address.com",
      "@AndrewSparrow"
    )

    val eventuallyMatches = dictionaryMatcher.check(MatcherRequest(nonWordExamples.map(getBlocks(_)).flatten))

    eventuallyMatches.map { matches =>
      matches shouldEqual Nil
    }
  }

  it should "report unknown words" in {
    val blocks = getBlocks("Example text with asdfghjk")
    val eventuallyMatches = dictionaryMatcher.check(MatcherRequest(blocks))

    eventuallyMatches.map { matches =>
      matches should matchTo(List(
        dictionaryMatcher.getRuleMatch("asdfghjk", 18, 26, Nil, blocks.head)
      ))
    }
  }

  it should "report multiple matches" in {
    val blocks = getBlocks("Example asdfghjk with John Simth")
    val eventuallyMatches = dictionaryMatcher.check(MatcherRequest(blocks))

    eventuallyMatches.map { matches =>
      matches should matchTo(List(
        dictionaryMatcher.getRuleMatch("asdfghjk", 8, 16, Nil, blocks.head),
        dictionaryMatcher.getRuleMatch("John Simth", 22, 32, List(TextSuggestion("John Smith")), blocks.head),
      ))
    }
  }

  it should "fuzzy match known names with suggestions" in {
    val blocks = getBlocks("Example text with John Simth")
    val eventuallyMatches = dictionaryMatcher.check(MatcherRequest(blocks))

    eventuallyMatches.map { matches =>
      matches should matchTo(List(
        dictionaryMatcher.getRuleMatch("John Simth", 18, 28, List(TextSuggestion("John Smith")), blocks.head),
      ))
    }
  }

  it should "produce correct ranges in names for blocks" in {
    val blocks = getBlocks("Jerry Brzeczek", 2)
    val eventuallyMatches = dictionaryMatcher.check(MatcherRequest(blocks))

    eventuallyMatches.map { matches =>
      matches should matchTo(List(
        dictionaryMatcher.getRuleMatch("Jerry Brzeczek", 0, 14, List(TextSuggestion("Jerzy Brzęczek")), blocks.head),
      ))
    }
  }

  it should "accept contractions in names" in {
    val blocks = getBlocks("Well, Eva Smith's gone. You can't do her any more harm.")
    val eventuallyMatches = dictionaryMatcher.check(MatcherRequest(blocks))

    eventuallyMatches.map { matches =>
      matches should matchTo(List(
        dictionaryMatcher.getRuleMatch("Eva Smith", 6, 15, Nil, blocks.head),
      ))
    }
  }

  it should "correct contractions in names" in {
    val blocks = getBlocks("Well, Eva Smiths gone. You can't do her any more harm.")
    val eventuallyMatches = dictionaryMatcher.check(MatcherRequest(blocks))

    eventuallyMatches.map { matches =>
      matches should matchTo(List(
        dictionaryMatcher.getRuleMatch("Eva Smiths", 6, 16, List(TextSuggestion("Eva Smith's"), TextSuggestion("Eva Smith")), blocks.head),
      ))
    }
  }

  it should "accept possessives in names" in {
    val blocks = getBlocks("It's what happened to her since she left Mr Birling's works that is important.")
    val eventuallyMatches = dictionaryMatcher.check(MatcherRequest(blocks))

    eventuallyMatches.map { matches =>
      matches should matchTo(List(
        dictionaryMatcher.getRuleMatch("Birling", 44, 51, Nil, blocks.head),
      ))
    }
  }

  it should "correct possessives in names" in {
    val blocks = getBlocks("It's what happened to her since she left Mr Birlings works that is important.")
    val eventuallyMatches = dictionaryMatcher.check(MatcherRequest(blocks))

    eventuallyMatches.map { matches =>
      matches should matchTo(List(
        dictionaryMatcher.getRuleMatch(
          "Birlings", 44, 52, List(TextSuggestion("Birling's"), TextSuggestion("Birling")), blocks.head),
      ))
    }
  }

  it should "create matches for known names" in {
    val blocks = getBlocks("I said she changed her name to Daisy Renton")
    val eventuallyMatches = dictionaryMatcher.check(MatcherRequest(blocks))

    eventuallyMatches.map { matches =>
      matches should matchTo(List(
        dictionaryMatcher.getRuleMatch("Daisy Renton", 31, 43, Nil, blocks.head)
      ))
    }
  }
}
