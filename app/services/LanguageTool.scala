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
  def createInstance(maybeLanguageModelDir: Option[File])(implicit ec: ExecutionContext): LanguageTool = {
    val language: Language = Languages.getLanguageForShortCode("en-GB")
    val cache: ResultCache = new ResultCache(10000)
    val userConfig: UserConfig = new UserConfig()

    maybeLanguageModelDir.foreach { languageModel =>
      SuggestionsOrdererConfig.setNgramsPath(languageModel.toString)
    }

    val instance = new JLanguageTool(language, cache, userConfig)
    maybeLanguageModelDir.foreach(instance.activateLanguageModelRules)
    new LanguageTool(instance)
  }
}

class LanguageTool(underlying: JLanguageTool)(implicit ec: ExecutionContext) {
  def check(text: String): Seq[RuleMatch] = {
    underlying.check(text).asScala.map(RuleMatch.fromLT)
  }

  def reingestRules(): Unit = {
    RuleManager.getAll.map(_.foreach(rule => underlying.addRule(PatternRule.toLT(rule))))
  }

  def getAllRules: List[PatternRule] = {
    underlying.getAllActiveRules.asScala.toList.flatMap(_ match {
      case patternRule: LTPatternRule => Some(PatternRule.fromLT(patternRule))
      case _ => None
    })
  }
}