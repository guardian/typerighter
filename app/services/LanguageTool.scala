package services

import model.RuleMatch
import org.languagetool._

import collection.JavaConverters._

object LanguageTool {
  def createInstance(): LanguageTool = {
    val language: Language = Languages.getLanguageForShortCode("en-GB")
    val cache: ResultCache = new ResultCache(10000)
    val userConfig: UserConfig = new UserConfig()
    val instance = new JLanguageTool(language, cache, userConfig)
    new LanguageTool(instance)
  }
}

class LanguageTool(underlying: JLanguageTool) {
  def check(text: String): Seq[RuleMatch] = {
    underlying.check(text).asScala.map(RuleMatch.fromLT)
  }
}