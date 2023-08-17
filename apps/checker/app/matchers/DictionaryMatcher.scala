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
  new SpellDictionaryBuilder().buildDictionary(rules.map(rule => rule.word))

  instance
    .getAllRules()
    .forEach(rule => {
      if (rule.getId() != MorfologikCollinsSpellerRule.RULE_ID) {
        instance.disableRule(rule.getId())
      } else {
        println(rule.getId())
      }
    })

  val matcher = new LanguageToolMatcher(instance)

  override def check(
      request: MatcherRequest
  )(implicit ec: ExecutionContext) = {
    matcher.check(request)
  }

  def getCategories() = instance.getAllActiveRules.asScala.toList.map { rule =>
    Category.fromLT(rule.getCategory)
  }.toSet

  def getType() = matcher.getType()

  override def getRules(): List[DictionaryRule] = rules
}
