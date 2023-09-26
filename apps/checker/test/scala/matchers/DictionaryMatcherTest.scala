package matchers

import com.gu.typerighter.model.{Category, DictionaryRule, TextBlock}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import services.MatcherRequest

class DictionaryMatcherTest extends AsyncFlatSpec with Matchers {
  "check" should "include groupKey" in {
    val exampleRule = DictionaryRule("123", "hello", Category("id", "desc"))
    val dictionaryValidator = new DictionaryMatcher(List(exampleRule))

    val text = "text"

    val eventuallyMatches = dictionaryValidator.check(
      MatcherRequest(List(TextBlock("text-block-id", text, 0, text.length)))
    )
    eventuallyMatches.map { matches =>
      matches.map(_.groupKey) shouldBe List(Some("MORFOLOGIK_RULE_COLLINS-text"))
    }
  }
}
