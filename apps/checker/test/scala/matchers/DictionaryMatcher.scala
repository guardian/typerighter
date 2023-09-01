package matchers

import com.gu.typerighter.model.{Category, DictionaryRule}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class DictionaryMatcherTest extends AsyncFlatSpec with Matchers {
  val dictionaryRules = List(
    DictionaryRule("1", "one", Category("fakeId", "fakeCategory")),
    DictionaryRule("2", "two", Category("fakeId", "fakeCategory")),
    DictionaryRule("3", "three", Category("fakeId", "fakeCategory"))
  )
  val matcher = new DictionaryMatcher(dictionaryRules)

  "isTitleCase" should "match a single title case word" in {
    val regexDoesMatch = matcher.isTitleCase("Word")

    regexDoesMatch shouldBe true
  }

  "isTitleCase" should "not match a single lower case word" in {
    val regexDoesMatch = matcher.isTitleCase("word")

    regexDoesMatch shouldBe false
  }

  "isTitleCase" should "not match a single ALL CAPS word" in {
    val regexDoesMatch = matcher.isTitleCase("WORD")

    regexDoesMatch shouldBe false
  }

  "isTitleCase" should "match title case words surrounded by spaces" in {
    val regexDoesMatch = matcher.isTitleCase("  Hey  Nice Marmot   ")

    regexDoesMatch shouldBe true
  }

  "isTitleCase" should "match title case words surrounded by spaces and hyphens" in {
    val regexDoesMatch = matcher.isTitleCase("Not-Exactly A Lightweight")

    regexDoesMatch shouldBe true
  }

  "isTitleCase" should "match title case words including apostrophes" in {
    val regexDoesMatch = matcher.isTitleCase("You're Not Wrong Walter")

    regexDoesMatch shouldBe true
  }

  "isTitleCase" should "match title case words containing accents" in {
    val regexDoesMatch = matcher.isTitleCase("Él Düderino")

    regexDoesMatch shouldBe true
  }

  "isTitleCase" should "not match mixed case word sequences" in {
    val regexDoesMatch = matcher.isTitleCase("Obviously You're Not a Golfer")

    regexDoesMatch shouldBe false
  }
}
