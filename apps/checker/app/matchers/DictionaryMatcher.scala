package matchers

import com.gu.typerighter.model.{Category, DictionaryRule}
import org.languagetool.{JLanguageTool, Language, ResultCache, UserConfig}
import services.collins.{CollinsEnglish, MorfologikCollinsSpellerRule, SpellDictionaryBuilder}
import services.MatcherRequest
import utils.Matcher

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.ListHasAsScala

class DictionaryMatcher(
    rules: List[DictionaryRule]
) extends Matcher {
  val language: Language = new CollinsEnglish()
  val cache: ResultCache = new ResultCache(10000)
  val userConfig: UserConfig = new UserConfig()

  val instance = new JLanguageTool(language, cache, userConfig)

  // As a side effect, make sure the .dict artefact is available to languageTool
  new SpellDictionaryBuilder().buildDictionary(rules.map(rule => rule.word))

  // Disable default LanguageTool rules in the instance, i.e. anything that
  // isn't a MORFOLOGIK_RULE_COLLINS
  instance
    .getAllRules()
    .forEach(rule => {
      if (rule.getId() != MorfologikCollinsSpellerRule.RULE_ID) {
        instance.disableRule(rule.getId())
      }
    })

  val matcher = new LanguageToolMatcher(instance)

  def isTitleCase(str: String): Boolean = {
    val upperCaseIncludingAccents = """[A-ZÀ-Ü]"""
    val lowerCaseIncludingAccents = """[a-zà-ü']"""
    val validSeparators = """(\s|\b|-)"""
    // The below regex should match a string of title case words with any number
    // of spaces, word boundaries, hyphens or apostrophes between them
    str.matches(
      raw"""($validSeparators*($upperCaseIncludingAccents$lowerCaseIncludingAccents*\b)$validSeparators*)+"""
    )
  }

  override def check(
      request: MatcherRequest
  )(implicit ec: ExecutionContext) = {
    val matcherResult = matcher.check(request)
    matcherResult.map(ruleMatches =>
      ruleMatches.filter(ruleMatch => !isTitleCase(ruleMatch.matchedText))
    )
  }

  def getCategories() = instance.getAllActiveRules.asScala.toList.map { rule =>
    Category.fromLT(rule.getCategory)
  }.toSet

  def getType() = matcher.getType()

  override def getRules(): List[DictionaryRule] = rules
}
