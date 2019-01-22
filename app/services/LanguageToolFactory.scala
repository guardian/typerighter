package services

import java.io.File

import model.{CheckQuery, PatternRule, RuleMatch}
import org.languagetool._
import org.languagetool.rules.patterns.{PatternRule => LTPatternRule}
import org.languagetool.rules.spelling.morfologik.suggestions_ordering.SuggestionsOrdererConfig

import collection.JavaConverters._
import scala.concurrent.ExecutionContext

class LanguageToolFactory(name: String, maybeLanguageModelDir: Option[File] = None) {
  def createInstance(): LanguageTool = {
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

  def getName = name
}

class LanguageTool(underlying: JLanguageTool) {
  def check(query: CheckQuery): Seq[RuleMatch] = {
    println(s"Running check on thread -- #${Thread.currentThread().getId} ${Thread.currentThread().getName}")
    underlying.check(query.text).asScala.map(RuleMatch.fromLT)
  }

  def reingestRules(implicit ec: ExecutionContext): Unit = {
    RuleManager.getAll.map(_.foreach(rule => underlying.addRule(PatternRule.toLT(rule))))
  }

  def getAllRules: List[PatternRule] = {
    underlying.getAllActiveRules.asScala.toList.flatMap(_ match {
      case patternRule: LTPatternRule => Some(PatternRule.fromLT(patternRule))
      case _ => None
    })
  }
}