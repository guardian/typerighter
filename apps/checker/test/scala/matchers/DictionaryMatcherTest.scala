package matchers

import com.gu.typerighter.model.{Category, DictionaryRule, TextBlock}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import services.MatcherRequest

class DictionaryMatcherTest extends AsyncFlatSpec with Matchers {
  "check" should "include groupKey" in {
    val exampleRule = DictionaryRule("123", "hello", Category("id", "desc"))
    val dictionaryValidator = new DictionaryMatcher(List(exampleRule))

    val eventuallyMatches = dictionaryValidator.check(
      MatcherRequest(
        List(
          TextBlock(
            id = "text-block-id",
            text = "text",
            from = 0,
            to = 4
          )
        )
      )
    )
    eventuallyMatches.map { matches =>
      matches.map(_.groupKey) shouldBe List(Some("MORFOLOGIK_RULE_COLLINS-text"))
    }
  }

  "check" should "exclude matches which correspond to named entities" in {
    val exampleRule = DictionaryRule("123", "hello", Category("id", "desc"))
    val dictionaryValidator = new DictionaryMatcher(List(exampleRule))

    val eventuallyMatches = dictionaryValidator.check(
      MatcherRequest(
        List(
          TextBlock(
            id = "text-block-id",
            text =
              "Guy Goma was interviewed by Karen Bowerman in London after staff confused him with Computer Life journalist Guy Kewney",
            from = 0,
            to = 118
          )
        )
      )
    )

    eventuallyMatches.map { matches =>
      matches.map(_.matchedText) shouldBe List(
        // "Guy Goma" (name) missing
        "was",
        "interviewed",
        "by",
        // "Karen Bowerman" (name) missing
        "in",
        // "London" (location) missing
        "after",
        "staff",
        "confused",
        "him",
        "with",
        // "Computer Life" (organisation) missing
        "journalist"
        // "Guy Kewney" (name) missing
      )
    }
  }
}
