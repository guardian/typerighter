package matchers

import java.io.File

import model.{LTRule, RuleMatch}
import org.languagetool._
import org.languagetool.rules.spelling.morfologik.suggestions_ordering.SuggestionsOrdererConfig
import org.languagetool.rules.{CategoryId, Rule => LanguageToolRule}
import play.api.Logging
import services.MatcherRequest
import utils.Matcher

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class LanguageToolFactory(
                           maybeLanguageModelDir: Option[File],
                           useLanguageModelRules: Boolean = false) extends Logging {

  def createInstance(category: String, rules: List[LTRule], defaultRuleIds: List[String])(implicit ec: ExecutionContext): (Matcher, List[String]) = {
    val language: Language = Languages.getLanguageForShortCode("en")
    val cache: ResultCache = new ResultCache(10000)
    val userConfig: UserConfig = new UserConfig()

    val instance = new JLanguageTool(language, cache, userConfig)

    maybeLanguageModelDir.foreach { languageModel =>
      SuggestionsOrdererConfig.setNgramsPath(languageModel.toString)
      if (useLanguageModelRules) instance.activateLanguageModelRules(languageModel)
    }

    // Disable all default rules by ... default
    instance.getAllRules().asScala.foreach((rule) => {
      println(s"${rule.getCategory()} ${rule.getId()}  ${rule.getDescription()}  ${rule.getIncorrectExamples().toString()}   ${rule.getCorrectExamples().toString()}")
    })


    // Add the rules provided in the config

    logger.info(s"Adding ${rules.size} rules to matcher instance ${category}")
    val ruleIngestionErrors = rules.flatMap { rule =>
      try {
        instance.addRule(LTRule.toLT(rule))
        None
      } catch {
        case e: Throwable => {
          Some(s"LanguageTool could not parse rule with id ${rule.id} and description ${rule.description}. The message was: ${e.getMessage}")
        }
      }
    }
    instance.enableRuleCategory(new CategoryId(category))

    (new LanguageToolMatcher(category, instance), ruleIngestionErrors)
  }
}


/**
  * A Matcher that wraps a LanguageTool instance.
  */
class LanguageToolMatcher(category: String, instance: JLanguageTool) extends Matcher {
  def getId = "language-tool"

  def getCategory = category

  def check(request: MatcherRequest)(implicit ec: ExecutionContext): Future[List[RuleMatch]] = {
    Future {
      request.blocks.flatMap { block =>
        instance.check(block.text).asScala.map(RuleMatch.fromLT(_, block)).toList.map { ruleMatch =>
          ruleMatch.copy(
            fromPos = ruleMatch.fromPos + block.from,
            toPos = ruleMatch.toPos + block.from
          )
        }
      }
    }
  }

  def getRules: List[LTRule] = {
    instance.getAllActiveRules.asScala.toList.flatMap(_ match {
      case rule: LanguageToolRule => Some(LTRule.fromLT(rule))
      case _ => None
    })
  }
}
