package matchers

import model._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffMatcher._
import services.MatcherRequest

class LanguageToolMatcherTest extends AsyncFlatSpec with Matchers {
  val exampleCategory = Category("EXAMPLE_CAT", "Example Category")
  val exampleRuleXml = """
    <rule>
      <pattern>
          <token postag="CD" />
          <token postag="NNS">
              <exception regexp="yes">centuries|decades|years|months|days|hours|minutes|seconds|stars</exception>
          </token>
          <token>or</token>
          <marker>
              <token>less</token>
          </marker>
      </pattern>
      <message>Did you mean <suggestion>fewer</suggestion>? The noun \2 is countable.</message>
      <short>Grammatical error</short>
      <example correction="fewer">Ten items or <marker>less</marker></example>
      <example>It typically takes 30 seconds or <marker>less</marker></example>
      <example>I would only give that hotel 3 stars or <marker>less</marker>.</example>
    </rule>
  """
  val exampleRule =  LTRuleXML(
    "EXAMPLE_RULE",
    exampleRuleXml,
    exampleCategory,
    "An example rule with custom XML"
  )
  val exampleRulegroupXml = """
    <rulegroup id="wrong_id" name="wrong_name" other="other_wrong_thing">
      <rule>
          <pattern>
          <token regexp="yes">(?i)[a-z]+</token>
          <token spacebefore="no" regexp="yes">…</token>
          <token spacebefore="no" regexp="yes">(?i)[a-z]+</token>
        </pattern>
        <message>Have spaces around ellipses</message>
        <suggestion><match no="1"/> … <match no="3"/></suggestion>
        <example>This is important – as far as I know.</example>
        <example correction="…">This is important<marker>...</marker> as far as I know.</example>
      </rule> 
      <rule>
        <pattern>
          <token regexp="yes">(January|February|March|April|May|June|July|August|September|October|November|December)</token>
          <token regexp="yes">(\d\d?)(th|rd|nd)?</token>
        </pattern>
        <message>Incorrect date format: <suggestion><match no="2" regexp_match="(\d\d?)(th|rd|nd)?" regexp_replace="$1"/> <match no="1"/></suggestion></message>
        <example correction="">It happened on <marker>3 November</marker> etc</example>
      </rule>  
      <rule>
        <pattern>
          <token regexp="yes">the</token>
          <token regexp="yes">(\d\d?)(th|rd|nd)</token>
          <token regexp="yes">of</token>
          <token regexp="yes">(January|February|March|April|May|June|July|August|September|October|November|December)</token>
        </pattern>
        <message>Incorrect date format: <suggestion><match no="2" regexp_match="(\d\d?)(th|rd|nd)" regexp_replace="$1"/> <match no="4"/></suggestion></message>
        <example correction="">It happened on <marker>3 November</marker> etc</example>
      </rule>
      <rule>
        <pattern>
          <token regexp="yes">(\d\d?)(th|rd|nd)</token>
          <token regexp="yes">(January|February|March|April|May|June|July|August|September|October|November|December)</token>
        </pattern>
        <message>Incorrect date format: <suggestion><match no="1" regexp_match="(\d\d?)(th|rd|nd)" regexp_replace="$1"/> <match no="2"/></suggestion></message>
        <example correction="">It happened on <marker>3 November</marker> etc</example>
      </rule>
  </rulegroup>
  """
  val exampleRulegroup =  LTRuleXML(
    "EXAMPLE_RULEGROUP",
    exampleRulegroupXml,
    exampleCategory,
    "An example rulegroup with custom XML"
  )

  "getInstance" should "provide no rules by default" in {
    val ltFactory = new LanguageToolFactory(None)
    val instance = ltFactory.createInstance(Nil).getOrElse(fail)
    instance.getRules() shouldBe Nil
  }

  "getInstance" should "include the rules we provide by id via `defaultRules`" in {
    val ltFactory = new LanguageToolFactory(None)
    val defaultRules = List("FEWER_LESS", "DOES_YOU")
    val instance = ltFactory.createInstance(Nil, defaultRules).getOrElse(fail)
    // These rule ids map to rule groups, which contain two rules each.
    // This is weird, as we'd assume ids to be unique. We may want to alter
    // this to reflect rule groupings to ensure id uniqueness, for example.
    instance.getRules().map(_.id) shouldBe List("FEWER_LESS", "FEWER_LESS", "DOES_YOU", "DOES_YOU")
  }

  "getInstance" should "include the XML-based rules we provide via `rules`" in {
    val ltFactory = new LanguageToolFactory(None)
    val exampleRules = List(exampleRule)
    val instance = ltFactory.createInstance(exampleRules).getOrElse(fail)
    instance.getRules().map(_.id) shouldBe List("EXAMPLE_RULE")
  }

  "getInstance" should "include the XML-based rulegroups we provide via `rules`" in {
    val ltFactory = new LanguageToolFactory(None)
    val exampleRulegroups = List(exampleRulegroup)
    val instance = ltFactory.createInstance(exampleRulegroups).getOrElse(fail)
    instance.getRules().map(_.id) shouldBe List("EXAMPLE_RULEGROUP", "EXAMPLE_RULEGROUP", "EXAMPLE_RULEGROUP", "EXAMPLE_RULEGROUP")
  }

  "getInstance" should "report categories both sorts of rules" in {
    val ltFactory = new LanguageToolFactory(None)
    val defaultRules = List("FEWER_LESS", "DOES_YOU")
    val exampleRules = List(exampleRule)
    val instance = ltFactory.createInstance(exampleRules, defaultRules).getOrElse(fail)
    instance.getCategories().map(_.id) shouldBe Set("GRAMMAR", "EXAMPLE_CAT")
  }

  "getInstance" should "handle cases where no novel rules are available" in {
    val ltFactory = new LanguageToolFactory(None)
    val instance = ltFactory.createInstance(List.empty).getOrElse(fail)
    instance.getRules().map(_.id) shouldBe List.empty
  }

  "check" should "apply LanguageTool default rules" in {
    val ltFactory = new LanguageToolFactory(None)
    val defaultRules = List("FEWER_LESS")
    val instance = ltFactory.createInstance(Nil, defaultRules).getOrElse(fail)
    val request = MatcherRequest(List(TextBlock("id-1", "Three or less tests passed!", 0, 29)))

    val eventuallyMatches = instance.check(request)
    val expectedMatchMessages = List("Did you mean <suggestion>fewer</suggestion>? The noun tests is countable.")
    eventuallyMatches map { matches =>
      matches.map(_.message) shouldBe expectedMatchMessages
    }
  }

  "check" should "apply LanguageTool custom rules" in {
    val ltFactory = new LanguageToolFactory(None)
    val exampleRules = List(exampleRule)
    val instance = ltFactory.createInstance(List(exampleRule)).getOrElse(fail)
    val request = MatcherRequest(List(TextBlock("id-1", "Three mistakes or less", 0, 29)))

    val eventuallyMatches = instance.check(request)
    val expectedMatchMessages = List("Did you mean <suggestion>fewer</suggestion>? The noun mistakes is countable.")
    eventuallyMatches map { matches =>
      matches.map(_.message) shouldBe expectedMatchMessages
    }
  }
}
