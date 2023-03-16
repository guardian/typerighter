package matchers

import com.gu.typerighter.model.{Category, LTRule, LTRuleXML, RuleMatch}
import java.io.File
import org.languagetool._
import org.languagetool.rules.spelling.morfologik.suggestions_ordering.SuggestionsOrdererConfig
import org.languagetool.rules.{Rule => LanguageToolRule}
import play.api.Logging
import services.MatcherRequest

import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}
import org.languagetool.rules.patterns.PatternRuleLoader
import org.languagetool.rules.patterns.AbstractPatternRule
import utils.{Matcher, MatcherCompanion}
import scala.xml.{XML, Attribute, Null, Text}
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.ByteArrayInputStream

class LanguageToolFactory(
    maybeLanguageModelDir: Option[File],
    useLanguageModelRules: Boolean = false
) extends Logging {

  def createInstance(ruleXMLs: List[LTRuleXML], defaultRuleIds: List[String] = Nil)(implicit
      ec: ExecutionContext
  ): Either[List[Throwable], Matcher] = {
    val language: Language = Languages.getLanguageForShortCode("en")
    val cache: ResultCache = new ResultCache(10000)
    val userConfig: UserConfig = new UserConfig()

    val instance = new JLanguageTool(language, cache, userConfig)

    maybeLanguageModelDir.foreach { languageModel =>
      SuggestionsOrdererConfig.setNgramsPath(languageModel.toString)
      if (useLanguageModelRules) instance.activateLanguageModelRules(languageModel)
    }

    // Disable all default rules ...
    val allRuleIds = instance.getAllRules.asScala.map(_.getId())
    allRuleIds.foreach(ruleId => instance.disableRule(ruleId))
    // ... apart from those we'd explicitly like
    val errors = defaultRuleIds.foldLeft(List.empty[Throwable])((acc, ruleId) =>
      if (allRuleIds.contains(ruleId)) {
        instance.enableRule(ruleId)
        acc
      } else
        new Exception(
          s"Attempted to enable a default rule with id ${ruleId}, but the rule was not available on the instance"
        ) :: acc
    )

    errors match {
      // Add custom rules
      case Nil =>
        applyXMLRules(instance, ruleXMLs) map { _ =>
          val matcher = new LanguageToolMatcher(instance)
          logger.info(
            s"Added ${ruleXMLs.size} rules and enabled ${defaultRuleIds.size} default rules for matcher instance with id: ${matcher.getId()}"
          )
          matcher
        }
      // Fail fast
      case e => Left(e)
    }
  }

  /** As a side-effect, apply the given ltRuleXmls to the given LanguageTool instance and enable
    * them for matching.
    */
  private def applyXMLRules(
      instance: JLanguageTool,
      ltRuleXmls: List[LTRuleXML]
  ): Either[List[Throwable], Unit] = {
    val maybeRuleErrors = getLTRulesFromXML(ltRuleXmls) match {
      case Success(rules) =>
        rules.map { rule =>
          val ruleTry = Try {
            instance.addRule(rule)
            instance.enableRule(rule.getId)
          }
          ruleTry.transform(
            s => Success(s),
            e => Failure(annotateExceptionWithRuleId(e, rule.getId))
          )
        }
      case Failure(e) => List(Failure(e))
    }

    maybeRuleErrors.flatMap {
      case Success(_) => None
      case Failure(e) => {
        logger.error(e.getMessage, e)
        Some(e)
      }
    } match {
      case Nil    => Right(())
      case errors => Left(errors)
    }
  }

  private def getLTRulesFromXML(rules: List[LTRuleXML]): Try[List[AbstractPatternRule]] =
    rules match {
      case Nil => Success(Nil)
      case r => {
        val loader = new PatternRuleLoader()
        getXMLStreamFromLTRules(rules) flatMap { xmlStream =>
          {
            Try(loader.getRules(xmlStream, "languagetool-generated-xml").asScala.toList)
          }
        }
      }
    }

  /** Attempt to get an XML stream representing a valid XML document containing the given rules.
    *
    * Fails if any of the rules contain invalid XML.
    */
  private def getXMLStreamFromLTRules(rules: List[LTRuleXML]): Try[ByteArrayInputStream] = Try {
    val rulesByCategory = rules.groupBy(_.category)
    val rulesXml = rulesByCategory.map { case (category, rules) =>
      <category id={category.id} name={category.name} type="grammar">
          {
        rules.map { rule =>
          try {
            XML.loadString(rule.xml) % Attribute(None, "id", Text(rule.id), Null) % Attribute(
              None,
              "name",
              Text(rule.description),
              Null
            )
          } catch {
            case e: Throwable =>
              val additionalMessage =
                s"Other rules for categories ${rulesByCategory.keys.map(_.id).mkString(", ")} have also been discarded"
              throw annotateExceptionWithRuleId(e, rule.id, additionalMessage)
          }
        }
      }
        </category>
    }

    // Temporarily hardcode language settings
    val ruleXml = <rules lang="en">{rulesXml}</rules>

    val outputStream = new ByteArrayOutputStream()
    val writer = new OutputStreamWriter(outputStream)
    XML.write(writer, ruleXml, "UTF-8", xmlDecl = true, doctype = xml.dtd.DocType("rules"))
    writer.close()

    new ByteArrayInputStream(outputStream.toByteArray())
  }

  private def annotateExceptionWithRuleId(
      e: Throwable,
      ruleId: String,
      additionalMessage: String = ""
  ) =
    new Exception(
      s"Error applying LanguageTool rule `${ruleId}`: ${e.getMessage} ${if (additionalMessage.nonEmpty) additionalMessage
        else ""}".trim,
      e
    )
}

object LanguageToolMatcher extends MatcherCompanion {
  def getType() = "languageTool"
}

/** A Matcher that wraps a LanguageTool instance.
  */
class LanguageToolMatcher(instance: JLanguageTool) extends Matcher {

  def getType() = LanguageToolMatcher.getType()

  def getCategories() = instance.getAllActiveRules.asScala.toList.map { rule =>
    Category.fromLT(rule.getCategory)
  }.toSet

  def check(request: MatcherRequest)(implicit ec: ExecutionContext): Future[List[RuleMatch]] =
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

  def getRules(): List[LTRule] = {
    instance.getAllActiveRules.asScala.toList.flatMap {
      case rule: LanguageToolRule => Some(LTRule.fromLT(rule))
      case _                      => None
    }
  }
}
