package matchers

import java.io.File

import model.{LTRule, LTRuleXML, RuleMatch}
import org.languagetool._
import org.languagetool.rules.spelling.morfologik.suggestions_ordering.SuggestionsOrdererConfig
import org.languagetool.rules.{CategoryId, Rule => LanguageToolRule}
import play.api.Logging
import services.MatcherRequest
import utils.{Matcher, MatcherCompanion}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import model.Category
import org.languagetool.rules.patterns.PatternRuleLoader
import org.languagetool.rules.patterns.PatternRule
import org.languagetool.rules.patterns.AbstractPatternRule
import scala.xml.XML
import scala.util.Try
import scala.util.Success
import scala.util.Failure

class LanguageToolFactory(
                           maybeLanguageModelDir: Option[File],
                           useLanguageModelRules: Boolean = false) extends Logging {

  def createInstance(category: Category, ruleXMLs: List[LTRuleXML], defaultRuleIds: List[String] = Nil)(implicit ec: ExecutionContext): (Matcher, List[String]) = {
    val language: Language = Languages.getLanguageForShortCode("en")
    val cache: ResultCache = new ResultCache(10000)
    val userConfig: UserConfig = new UserConfig()

    val instance = new JLanguageTool(language, cache, userConfig)

    maybeLanguageModelDir.foreach { languageModel =>
      SuggestionsOrdererConfig.setNgramsPath(languageModel.toString)
      if (useLanguageModelRules) instance.activateLanguageModelRules(languageModel)
    }

    logger.info(s"Adding ${ruleXMLs.size} rules and enabling ${defaultRuleIds.size} default rules to matcher instance ${category}")

    // Disable all default rules, apart from those we'd explicitly like
    instance.getAllRules().asScala.foreach { rule =>
      if (!defaultRuleIds.contains(rule.getId())) {
        instance.disableRule(rule.getId())
      }
    }

    // Add custom rules
    val maybeRuleErrors = getRulesFromXML(ruleXMLs) match {
      case Success(rules) => {
        rules.map { rule => Try(instance.addRule(rule)) }
      }
      case Failure(e) => List(Failure(e))
    }
    val ruleErrors = maybeRuleErrors.flatMap {
      case Success(_) => None
      case Failure(e) => {
        logger.error(e.getMessage(), e)
        Some(e.getMessage())
      }
    }

    instance.enableRuleCategory(new CategoryId(category.id))

    (new LanguageToolMatcher(category, instance), ruleErrors)
  }

  private def getRulesFromXML(rules: List[LTRuleXML]): Try[List[AbstractPatternRule]] = {
    val loader = new PatternRuleLoader()
    getXMLStreamFromLTRules(rules) flatMap {
      xmlStream => Try(loader.getRules(xmlStream, "languagetool-generated-xml").asScala.toList)
    }
  }

  private def getXMLStreamFromLTRules(rules: List[LTRuleXML]): Try[java.io.ByteArrayInputStream] = {
    Try {
      val rulesXml = rules.map(rule =>
        <rule id={rule.id} name={rule.description}>
          {XML.loadString(rule.xml)}
        </rule>
      )
      val ruleXml = <rules>{rulesXml}</rules>
      val bytes = rulesXml.toString.getBytes(java.nio.charset.StandardCharsets.UTF_8.name)
      new java.io.ByteArrayInputStream(bytes)
    }
  }
}

object LanguageToolMatcher extends MatcherCompanion {
  def getType = "languageTool"
}

/**
  * A Matcher that wraps a LanguageTool instance.
  */
class LanguageToolMatcher(category: Category, instance: JLanguageTool) extends Matcher {

  def getType = LanguageToolMatcher.getType
  def getCategory = category

  def check(request: MatcherRequest)(implicit ec: ExecutionContext): Future[List[RuleMatch]] = Future {
    request.blocks.flatMap { block =>
      instance.check(block.text).asScala.map(RuleMatch.fromLT(_, block, getType)).toList.map { ruleMatch =>
        ruleMatch.copy(
          fromPos = ruleMatch.fromPos + block.from,
          toPos = ruleMatch.toPos + block.from
        )
      }
    }
  }

  def getRules: List[LTRule] = {
    instance.getAllActiveRules.asScala.toList.flatMap {
      case rule: LanguageToolRule => Some(LTRule.fromLT(rule))
      case _ => None
    }
  }
}
