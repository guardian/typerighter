package model

import java.util.{List => JList}
import java.util.regex.Pattern

import play.api.libs.json.{JsObject, Json, JsPath, JsResult, JsString, JsSuccess, Reads, Writes}
import play.api.libs.json._
import play.api.libs.json.Reads._

import org.languagetool.Languages
import org.languagetool.rules.patterns.{PatternRule => LTPatternRule, PatternToken => LTPatternToken}
import org.languagetool.rules.{Rule => LanguageToolRule}

import scala.util.matching.Regex
import scala.collection.JavaConverters._

import utils.Text
import matchers.RegexMatcher

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
  implicit val writes: Writes[BaseRule] = Json.writes[BaseRule]
  implicit val reads: Reads[BaseRule] = Json.reads[BaseRule]
}

object RegexRule {
  implicit val regexWrites: Writes[Regex] = (regex: Regex) => JsString(regex.toString)
  implicit val regexReads: Reads[Regex] = (JsPath).read[String].map(new Regex(_))
  implicit val writes: Writes[RegexRule] = Json.writes[RegexRule]
  implicit val reads: Reads[RegexRule] = Json.reads[RegexRule]
}

case class RegexRule(
    id: String,
    category: Category,
    description: String,
    suggestions: List[TextSuggestion] = List.empty,
    replacement: Option[TextSuggestion] = None,
    regex: Regex
) extends BaseRule {

  def toMatch(start: Int, end: Int, block: TextBlock, isStartOfSentence: Boolean = false): RuleMatch = {
    val matchedText = block.text.substring(start, end)
    val transformedReplacement = replacement.map { r =>
      r
        .replaceAllIn(regex, matchedText)
        .ensureCorrectCase(isStartOfSentence)
    }
    val (precedingText, subsequentText) = Text.getSurroundingText(block.text, start, end)

    RuleMatch(
      rule = this,
      fromPos = start + block.from,
      toPos = end + block.from,
      precedingText = precedingText,
      subsequentText = subsequentText,
      matchedText = matchedText,
      message = description,
      shortMessage = Some(description),
      suggestions = suggestions,
      replacement = transformedReplacement,
      markAsCorrect = transformedReplacement.map(_.text).getOrElse("") == block.text.substring(start, end),
      matchContext = Text.getMatchTextSnippet(precedingText, matchedText, subsequentText),
      matcherType = RegexMatcher.getType
    )
  }
}

/**
  * The application's representation of a LanguageTool PatternRule as expressed
  * in XML. Consumed directly by the LanguageToolMatcher to produce a rule.
  */
case class LTRuleXML(
  id: String,
  xml: String,
  category: Category,
  description: String
) extends BaseRule {
  val suggestions = List.empty
  val replacement: Option[TextSuggestion] = None
}

object LTRuleXML {
  implicit val writes: Writes[LTRuleXML] = Json.writes[LTRuleXML]
  implicit val reads: Reads[LTRuleXML] = Json.reads[LTRuleXML]
}

/**
  * The application's representation of a LanguageTool PatternRule. Used to
  * (lossily) present a LanguageTool PatternRule in a UI.
  */
case class LTRule(id: String,
                  category: Category,
                  languageShortcode: Option[String] = None,
                  patternTokens: Option[List[PatternToken]] = None,
                  pattern: Option[Pattern] = None,
                  description: String,
                  message: String,
                  url: Option[String] = None,
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

object NameRule {
  implicit val writes: Writes[NameRule] = Json.writes[NameRule]
  implicit val reads: Reads[NameRule] = Json.reads[NameRule]
}

sealed trait Pronoun {
  val id: String
}

object Pronoun {
  implicit val writes: Writes[Pronoun] = (pronoun: Pronoun) => JsString(pronoun.id)
  implicit val reads: Reads[Pronoun] = (JsPath).read[String].map {
    case "HE_HIS" => HE_HIS
    case "SHE_HERS" => SHE_HERS
    case "THEY_THEM" => THEY_THEM
    case _ => UNKNOWN
  }
}

case object HE_HIS extends Pronoun {
  val id = "HE_HIS"
}
case object SHE_HERS extends Pronoun {
  val id = "SHE_HERS"
}
case object THEY_THEM extends Pronoun {
  val id = "THEY_THEM"
}
case object UNKNOWN extends Pronoun {
  val id = "UNKNOWN"
}

case class NameRule(
    id: String,
    firstName: String,
    lastName: String,
    pronoun: Pronoun,
    category: Category,
    description: String
) extends BaseRule {
  val replacement = None
  val suggestions: List[Suggestion] = List.empty
}
