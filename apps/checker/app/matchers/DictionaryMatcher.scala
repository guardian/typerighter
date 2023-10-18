package matchers

import com.gu.typerighter.model.{Category, DictionaryRule, RuleMatch}
import com.google.common.cache.LoadingCache
import morfologik.stemming.Dictionary
import org.languagetool.{JLanguageTool, Language, ResultCache, UserConfig}
import play.api.Logging
import services.collins.{CollinsEnglish, MorfologikCollinsSpellerRule, SpellDictionaryBuilder}
import services.{EntityHelper, EntityInText, MatcherRequest}
import utils.Matcher

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.ListHasAsScala

class DictionaryMatcher(
    rules: List[DictionaryRule],
    entityHelper: EntityHelper
) extends Matcher
    with Logging {
  val language: Language = new CollinsEnglish()
  val cache: ResultCache = new ResultCache(10000)
  val userConfig: UserConfig = new UserConfig()

  // As a side effect, make sure the .dict artefact is available to languageTool
  new SpellDictionaryBuilder().buildDictionary(rules.map(rule => rule.word))

  // This is an undesirable hack to force the MorfologikSpeller dictionary cache to invalidate. MorfologikSpeller
  // is quite deep in a series of classes:
  //   - MorfologikCollinsSpellerRule extends AbstractSpellerRule extends MorfologikSpellerRule which uses an
  //     instance of MorfologikMultiSpeller which uses an instance of Morfologik Speller
  // We don't want to re-implement all of those classes.
  // Without invalidating the dictionary cache expires every 10 minutes, changes to dictionary rules will not be
  // reflected in the DictionaryMatcher until a future re-compile of the dictionary when at least 10 minutes
  // have expired. We want to invalidate the cache as soon as our DictionaryRules change.
  // Ideally we would make changes to the LanguageTool library to allow this, but as a temporary hack, this allows
  // our DictionaryMatcher to react to new or edited DictionaryRules.
  val SpellerClass = Class.forName("org.languagetool.rules.spelling.morfologik.MorfologikSpeller")
  val field = SpellerClass.getDeclaredField("dictCache")
  field.setAccessible(true)
  val dictionaryCache = field.get(field.getType()).asInstanceOf[LoadingCache[String, Dictionary]]
  dictionaryCache.invalidateAll()

  val instance = new JLanguageTool(language, cache, userConfig)

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
      entity.range.from <= ruleMatch.fromPos && entity.range.to >= ruleMatch.toPos
    )
  }

  def matchFallsWithinNamedEntity(
      ruleMatch: RuleMatch,
      entities: List[String]
  ): Boolean = {
    entities.exists(entity => entity contains ruleMatch.matchedText)
  }

  override def check(
      request: MatcherRequest
  )(implicit ec: ExecutionContext): Future[List[RuleMatch]] = {
    val wholeArticle = request.blocks.map(block => block.text).mkString("\n")
    val eventualNamedEntities = entityHelper.getEntityResultFromNERService(text = wholeArticle)

    for {
      namedEntities <- eventualNamedEntities
      matches <- matcher.check(request)
    } yield {
      matches
        // Remove matches which correspond to named entities. This should reduce the number of false-positives
        // caused by proper nouns
        .filter(ruleMatch => {
          namedEntities match {
            case Left(error) =>
              logger.error(s"NER check failed with message: ${error.getMessage}")
              true
            case Right(entities) =>
              val shouldIncludeMatch = !matchFallsWithinNamedEntity(ruleMatch, entities)
              if (!shouldIncludeMatch) {
                logger.info(
                  s"Dropping match for ruleId: ${ruleMatch.rule.id} for text: ${ruleMatch.precedingText}[${ruleMatch.matchedText}]${ruleMatch.subsequentText}, as it's been tagged as an entity"
                )
              }
              shouldIncludeMatch
          }

        })
        // groupKey is used to control how rules are grouped in the client when they produces matches.
        // This is needed for dictionary matches as they all share a common rule ID (MORFOLOGIK_RULE_COLLINS)
        // groupKeys for dictionary matches have the format `MORFOLOGIK_RULE_COLLINS-{matchedText}`
        .map(ruleMatch =>
          ruleMatch
            .copy(groupKey = Some(ruleMatch.rule.id + '-' + ruleMatch.matchedText), priority = 0)
        )
    }
  }

  def getCategories() = instance.getAllActiveRules.asScala.toList.map { rule =>
    Category.fromLT(rule.getCategory)
  }.toSet

  def getType() = matcher.getType()

  override def getRules(): List[DictionaryRule] = rules
}
