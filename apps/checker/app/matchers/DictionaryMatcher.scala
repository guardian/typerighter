package matchers

import com.gu.typerighter.model.{Category, DictionaryRule, RuleMatch}
import org.languagetool.{JLanguageTool, Language, ResultCache, UserConfig}
import services.collins.{CollinsEnglish, MorfologikCollinsSpellerRule, SpellDictionaryBuilder}
import services.{EntityHelper, EntityInText, MatcherRequest}
import utils.Matcher

import scala.concurrent.{ExecutionContext, Future}
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

  def matchFallsWithinNamedEntityRange(
      ruleMatch: RuleMatch,
      entities: List[EntityInText]
  ): Boolean = {
    entities.exists(entity =>
      entity.range.from == ruleMatch.fromPos && entity.range.to == ruleMatch.toPos
    )
  }

  override def check(
      request: MatcherRequest
  )(implicit ec: ExecutionContext): Future[List[RuleMatch]] = {
    val entityHelper = new EntityHelper()
    val namedEntities =
      request.blocks.flatMap((block) => entityHelper.getEntitiesFromText(block.text))

    matcher
      .check(request)
      .map(ruleMatches =>
        ruleMatches
          // Remove matches which correspond to named entities. This should reduce the number of false-positives
          // caused by pronouns
          .filter(ruleMatch => !matchFallsWithinNamedEntityRange(ruleMatch, namedEntities))
          // groupKey is used to control how rules are grouped in the client when they produces matches.
          // This is needed for dictionary matches as they all share a common rule ID (MORFOLOGIK_RULE_COLLINS)
          // groupKeys for dictionary matches have the format `MORFOLOGIK_RULE_COLLINS-{matchedText}`
          .map(ruleMatch =>
            ruleMatch.copy(groupKey = Some(ruleMatch.rule.id + '-' + ruleMatch.matchedText))
          )
      )
  }

  def getCategories() = instance.getAllActiveRules.asScala.toList.map { rule =>
    Category.fromLT(rule.getCategory)
  }.toSet

  def getType() = matcher.getType()

  override def getRules(): List[DictionaryRule] = rules
}
