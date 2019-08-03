package services

import java.io.File

import org.languagetool._
import org.languagetool.rules.{Rule => LTRule}
import org.languagetool.rules.spelling.morfologik.suggestions_ordering.SuggestionsOrdererConfig

import collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger
import model.RuleMatch
import model.Rule
import org.languagetool.rules.CategoryId
import utils.Validator

class LanguageToolFactory(
                           maybeLanguageModelDir: Option[File],
                           useLanguageModelRules: Boolean = false) extends ValidatorFactory {

  def createInstance(category: String, config: ValidatorConfig)(implicit ec: ExecutionContext): (Validator, List[String]) = {
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

    // Add the rules provided in the config
    Logger.info(s"Adding ${config.rules.size} rules to validator instance ${category}")
    val ruleIngestionErrors = config.rules.flatMap { rule =>
      try {
        instance.addRule(Rule.toLT(rule))
        None
      } catch {
        case e: Throwable => {
          Some(s"LanguageTool could not parse rule with id ${rule.id} and description ${rule.description}. The message was: ${e.getMessage}")
        }
      }
    }
    instance.enableRuleCategory(new CategoryId(category))

    (new LanguageTool(category, instance), ruleIngestionErrors)
  }
}

class LanguageTool(category: String, instance: JLanguageTool)(implicit ec: ExecutionContext) extends Validator {
  def getId = "language-tool"

  def getCategory = category

  def check(request: ValidatorRequest): Future[List[RuleMatch]] = {
    Future.successful(instance.check(request.text).asScala.map(RuleMatch.fromLT).toList)
  }

  def getRules: List[Rule] = {
    instance.getAllActiveRules.asScala.toList.flatMap(_ match {
      case rule: LTRule => Some(Rule.fromLT(rule))
      case _ => None
    })
  }
}