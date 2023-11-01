package matchers

import com.gu.typerighter.model.{Category, DictionaryRule, TextBlock, TextRange}
import org.mockito.Mockito.when
import org.mockito.MockitoSugar.mock
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import services.{EntityHelper, EntityInText, MatcherRequest}

import scala.concurrent.Future

class DictionaryMatcherTest extends AsyncFlatSpec with Matchers {
  "check" should "include groupKey" in {
    val exampleRule = DictionaryRule("123", "hello", Category("id", "desc"))

    // We don't want to make actual API requests so we mock entityHelper here
    val entityHelper = mock[EntityHelper]
    when(entityHelper.getEntityResultFromNERService("text")).thenReturn(Future { Right(List()) })

    val dictionaryValidator = new DictionaryMatcher(List(exampleRule), entityHelper)

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

    // We don't want to make actual API requests so we mock entityHelper here
    val entityHelper = mock[EntityHelper]
    when(
      entityHelper.getEntityResultFromNERService(
        "Guy Goma was interviewed by Karen Bowerman in London after staff confused him with Computer Life journalist Guy Kewney"
      )
    ).thenReturn(
      Future {
        Right(
          List(
            EntityInText(word = "Guy Goma", range = TextRange(from = 0, to = 8)),
            EntityInText(word = "Karen Bowerman", range = TextRange(from = 28, to = 42)),
            EntityInText(word = "London", range = TextRange(from = 46, to = 52)),
            EntityInText(word = "Computer Life", range = TextRange(from = 83, to = 96)),
            EntityInText(word = "Guy Kewney", range = TextRange(from = 108, to = 118))
          )
        )
      }
    )

    val dictionaryValidator = new DictionaryMatcher(List(exampleRule), entityHelper)

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
