package services

import java.io.File

import model.RuleMatch
import model.PatternRule
import org.languagetool._
import org.languagetool.rules.patterns.{PatternRule => LTPatternRule}
import org.languagetool.rules.spelling.morfologik.suggestions_ordering.SuggestionsOrdererConfig

import collection.JavaConverters._
import scala.concurrent.ExecutionContext

object LanguageTool {
  def createInstance(maybeLanguageModelDir: Option[File], useLanguageModelRules: Boolean = false)(implicit ec: ExecutionContext): LanguageTool = {
    val language: Language = Languages.getLanguageForShortCode("en")
    val cache: ResultCache = new ResultCache(10000)
    val userConfig: UserConfig = new UserConfig()

    val instance = new JLanguageTool(language, cache, userConfig)

    maybeLanguageModelDir.foreach { languageModel =>
      SuggestionsOrdererConfig.setNgramsPath(languageModel.toString)
      if (useLanguageModelRules) instance.activateLanguageModelRules(languageModel)
    }

    // Disable all default rules by ... default
    instance.getCategories().asScala.foreach((categoryData) => instance.disableCategory(categoryData._1))

    new LanguageTool(instance)
  }
}

class LanguageTool(instance: JLanguageTool)(implicit ec: ExecutionContext) {
  def check(text: String): Seq[RuleMatch] = {
    instance.check(text).asScala.map(RuleMatch.fromLT)
  }

  /**
    * Reingest the given rules, removing any existing rules. Returns a list
    * containing error data for each rule that couldn't be added.
    */
  def reingestRules(rules: List[PatternRule]): List[String] = {
    rules.foldLeft(List.empty[String])((acc, rule) => {
      try {
        instance.addRule(PatternRule.toLT(rule))
        acc
      } catch {
        case e: Throwable => {
          acc :+ s"LanguageTool could not parse rule with id ${rule.id} and description ${rule.description}. The message was: ${e.getMessage}"
        }
      }
    })
  }

  def getAllRules: List[PatternRule] = {
    instance.getAllActiveRules.asScala.toList.flatMap(_ match {
      case patternRule: LTPatternRule => Some(PatternRule.fromLT(patternRule))
      case _ => None
    })
  }
}