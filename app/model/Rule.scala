package model

import java.util.{List => JList}
import java.util.regex.Pattern

import org.languagetool.Languages
import org.languagetool.rules.patterns.{PatternRule => LTPatternRule, PatternToken => LTPatternToken}
import org.languagetool.rules.{Rule => LTRule}
import play.api.libs.json._
import play.api.libs.json.Reads._
import services.LTRegexPatternRule

import scala.collection.JavaConverters._

/**
  * The application's representation of a LanguageTool PatternRule.
  */
case class Rule(id: String,
                category: Category,
                languageShortcode: Option[String],
                patternTokens: Option[List[PatternToken]] = None,
                pattern: Option[Pattern] = None,
                description: String,
                message:String,
                url: Option[String],
                suggestions: List[String])

object Rule {
  def fromLT(lt: LTRule): Rule = {
    val maybePatternTokens = lt match {
      case rule: LTPatternRule => Some(rule.getPatternTokens.asScala
        .toList
        .map(PatternToken.fromLT(_)))
      case _ => None
    }

    val maybeLanguageShortcode = lt match {
      case rule: LTPatternRule => Some(rule.getLanguage.getShortCode)
      case _ => None
    }

    Rule(
      id = lt.getId,
      category = Category.fromLT(lt.getCategory),
      languageShortcode = maybeLanguageShortcode,
      patternTokens = maybePatternTokens,
      pattern = None,
      description = lt.getDescription,
      message = lt.getDescription,
      url = Option(if (lt.getUrl == null) null else lt.toString),
      suggestions = List.empty
    )
  }

  def toLT(rule: Rule): LTRule = {
    val patternTokens: JList[LTPatternToken] = seqAsJavaList(rule.patternTokens.getOrElse(List[PatternToken]()).map(PatternToken.toLT))
    val language = Languages.getLanguageForShortCode(rule.languageShortcode.getOrElse("en-GB"))
    val message = rule.suggestions match {
      case Nil => rule.message
      case suggestions => {
        val ruleMessage = if (rule.message == "") "" else rule.message + ". "
        ruleMessage.concat(suggestions.map(s => s"<suggestion>${s}</suggestion>").mkString(", "))
      }
    }
    val ltRule = rule.pattern match {
      case Some(pattern) => {
        LTRegexPatternRule.getInstance(
          rule.id,
          rule.description,
          message,
          "",
          language,
          pattern,
          0
        )
      }
      case _ => {
        new LTPatternRule(
          rule.id,
          language,
          patternTokens,
          rule.description,
          message,
          rule.message
        )
      }
    }
    ltRule.setCategory(Category.toLT(rule.category))
    ltRule
  }

  implicit val patternWrites = new Writes[Pattern] {
    def writes(pattern: Pattern) = JsString(pattern.toString)
  }
  implicit val patternReads: Reads[Pattern] = JsPath.read[String].map {
    regex => Pattern.compile(regex)
  }

  implicit val writes: Writes[Rule] = Json.writes[Rule]
  implicit val reads: Reads[Rule] = Json.reads[Rule]
}
