package model

import java.util.{List => JList}
import java.util.regex.Pattern

import org.languagetool.Languages
import org.languagetool.rules.patterns.{PatternRule => LTPatternRule, PatternToken => LTPatternToken}
import org.languagetool.rules.{Rule => LanguageToolRule}
import play.api.libs.json._
import play.api.libs.json.Reads._

import scala.util.matching.Regex
import scala.collection.JavaConverters._

/**
  * A rule to match text against.
  */
sealed trait BaseRule {
  val id: String
  val category: Category
  val description: String
  val suggestions: List[Suggestion]
  val replacement: Option[TextSuggestion]
}

object BaseRule {
  implicit val writes: Writes[BaseRule] = {
    case r: RegexRule => RegexRule.writes.writes(r)
    case r: LTRule => LTRule.writes.writes(r)
  }
}

object RegexRule {
  implicit val regexWrites: Writes[Regex] = (regex: Regex) => JsString(regex.toString)
  implicit val writes: Writes[RegexRule] = Json.writes[RegexRule]
}

case class RegexRule(
    id: String,
    category: Category,
    description: String,
    suggestions: List[TextSuggestion] = List.empty,
    replacement: Option[TextSuggestion] = None,
    regex: Regex
) extends BaseRule

/**
  * The application's representation of a LanguageTool PatternRule.
  */
case class LTRule(id: String,
                  category: Category,
                  languageShortcode: Option[String],
                  patternTokens: Option[List[PatternToken]] = None,
                  pattern: Option[Pattern] = None,
                  description: String,
                  message: String,
                  url: Option[String],
                  suggestions: List[TextSuggestion]) extends BaseRule {
  val replacement: Option[TextSuggestion] = None
}

object LTRule {
  def fromLT(lt: LanguageToolRule): LTRule = {
    val maybePatternTokens = lt match {
      case rule: LTPatternRule => Some(rule.getPatternTokens.asScala
        .toList
        .map(PatternToken.fromLT)
      )
      case _ => None
    }

    val maybeLanguageShortcode = lt match {
      case rule: LTPatternRule => Some(rule.getLanguage.getShortCode)
      case _ => None
    }

    LTRule(
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

  def toLT(rule: LTRule): LanguageToolRule = {
    val patternTokens: JList[LTPatternToken] = seqAsJavaList(rule.patternTokens.getOrElse(List[PatternToken]()).map(PatternToken.toLT))
    val language = Languages.getLanguageForShortCode(rule.languageShortcode.getOrElse("en-GB"))
    val message = rule.suggestions match {
      case Nil => rule.message
      case suggestions => {
        val ruleMessage = if (rule.message == "") "" else rule.message + ". "
        ruleMessage.concat(suggestions.map(s => s"<suggestion>${s.text}</suggestion>").mkString(", "))
      }
    }

    val ltRule = new LTPatternRule(
      rule.id,
      language,
      patternTokens,
      rule.description,
      message,
      rule.message
    )

    ltRule.setCategory(Category.toLT(rule.category))
    ltRule
  }

  implicit val patternWrites = new Writes[Pattern] {
    def writes(pattern: Pattern) = JsString(pattern.toString)
  }
  implicit val patternReads: Reads[Pattern] = JsPath.read[String].map {
    regex => Pattern.compile(regex)
  }

  implicit val writes: Writes[LTRule] = Json.writes[LTRule]
  implicit val reads: Reads[LTRule] = Json.reads[LTRule]
}
