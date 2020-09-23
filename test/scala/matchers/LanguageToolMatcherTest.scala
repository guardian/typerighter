package matchers

import model._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffMatcher._
import services.MatcherRequest

class LanguageToolMatcherTest extends AsyncFlatSpec with Matchers {
  val exampleCategory = Category("EXAMPLE_CAT", "Example Category", "puce")
  val exampleRule =  LTRule(
    "EXAMPLE_RULE",
    exampleCategory,
    None,
    None,
    None,
    "Example description",
    "Example message",
    None,
    Nil
  )

  "getInstance" should "provide no rules by default" in {
    val ltFactory = new LanguageToolFactory(None)
    val (instance, _) = ltFactory.createInstance(exampleCategory, Nil, Nil)
    instance.getRules() shouldBe Nil
  }

  "getInstance" should "include the rules we provide by id via `defaultRules`" in {
    val ltFactory = new LanguageToolFactory(None)
    val defaultRules = List("FEWER_LESS", "DOES_YOU")
    val (instance, _) = ltFactory.createInstance(exampleCategory, Nil, defaultRules)
    // These rule ids map to rule groups, which contain two rules each.
    // This is weird, as we'd assume ids to be unique. We may want to alter
    // this to reflect rule groupings to ensure id uniqueness, for example.
    instance.getRules().map(_.id) shouldBe List("FEWER_LESS", "FEWER_LESS", "DOES_YOU", "DOES_YOU")
  }

  "getInstance" should "include the rules we provide via LTRules via `rules`" in {
    val ltFactory = new LanguageToolFactory(None)
    val exampleRules = List(exampleRule)
    val (instance, _) = ltFactory.createInstance(exampleCategory, exampleRules, Nil)
    instance.getRules().map(_.id) shouldBe List("EXAMPLE_RULE")
  }

  "check" should "apply LanguageTool rules" in {
    val ltFactory = new LanguageToolFactory(None)
    val defaultRules = List("FEWER_LESS")
    val (instance, _) = ltFactory.createInstance(exampleCategory, Nil, defaultRules)
    val request = MatcherRequest(List(TextBlock("id-1", "Three or less tests passed!", 0, 29)), "EXAMPLE_CAT")

    val eventuallyMatches = instance.check(request)
    val expectedMatchMessages = List("Did you mean <suggestion>fewer</suggestion>? The noun tests is countable.")
    eventuallyMatches map { matches =>
      matches.map(_.message) shouldBe expectedMatchMessages
    }
  }
}
