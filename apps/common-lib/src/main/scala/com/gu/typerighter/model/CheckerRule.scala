package com.gu.typerighter.model

import java.util.{List => JList}
import java.util.regex.Pattern
import play.api.libs.json.{JsPath, JsString, Json, OFormat, Reads, Writes}
import play.api.libs.json.Reads._
import org.languagetool.Languages
import org.languagetool.rules.patterns.{
  PatternRule => LTPatternRule,
  PatternToken => LTPatternToken
}
import org.languagetool.rules.{Rule => LanguageToolRule}

import scala.util.matching.Regex
import scala.jdk.CollectionConverters._

/** A rule to match text against.
  */
sealed trait CheckerRule {
  val id: String
  val category: Category
}

object CheckerRule {
  implicit val writes: Writes[CheckerRule] = Json.writes[CheckerRule]
  implicit val reads: Reads[CheckerRule] = Json.reads[CheckerRule]
}

case class LTRuleCore(
    id: String,
    languageToolRuleId: String
) extends CheckerRule {
  override val category: Category = Category("lt_core", "LanguageTool Core Rule")
}

object LTRuleCore {
  implicit val writes: Writes[LTRuleCore] = Json.writes[LTRuleCore]
  implicit val reads: Reads[LTRuleCore] = Json.reads[LTRuleCore]
}

object RegexRule {
  implicit val regexWrites: Writes[ComparableRegex] = (regex: ComparableRegex) =>
    JsString(regex.toString())
  implicit val regexReads: Reads[ComparableRegex] = JsPath.read[String].map(new ComparableRegex(_))
  implicit val writes: Writes[RegexRule] = Json.writes[RegexRule]
  implicit val reads: Reads[RegexRule] = Json.reads[RegexRule]
}

class ComparableRegex(regex: String, groupNames: String*) extends Regex(regex, groupNames: _*) {
  override def equals(obj: Any): Boolean = {
    obj match {
      case r: ComparableRegex =>
        this.pattern.pattern == r.pattern.pattern &&
        this.pattern.flags == r.pattern.flags
      case _ => false
    }
  }

  override def hashCode(): Int = this.pattern.##
}

case class RegexRule(
    id: String,
    category: Category,
    description: String,
    suggestions: List[TextSuggestion] = List.empty,
    replacement: Option[TextSuggestion] = None,
    regex: ComparableRegex
) extends CheckerRule {

  def toMatch(
      start: Int,
      end: Int,
      block: TextBlock,
      isStartOfSentence: Boolean = false
  ): RuleMatch = {
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
      markAsCorrect =
        transformedReplacement.map(_.text).getOrElse("") == block.text.substring(start, end),
      matchContext = Text.getMatchTextSnippet(precedingText, matchedText, subsequentText)
    )
  }
}

/** The application's representation of a LanguageTool PatternRule as expressed in XML. Consumed
  * directly by the LanguageToolMatcher to produce a rule.
  */
case class LTRuleXML(
    id: String,
    xml: String,
    category: Category,
    description: String
) extends CheckerRule {
  val suggestions = List.empty
  val replacement: Option[TextSuggestion] = None
}

object LTRuleXML {
  implicit val writes: Writes[LTRuleXML] = Json.writes[LTRuleXML]
  implicit val reads: Reads[LTRuleXML] = Json.reads[LTRuleXML]
}

/** The application's representation of a LanguageTool PatternRule. Used to (lossily) present a
  * LanguageTool PatternRule in a UI.
  */
case class LTRule(
    id: String,
    category: Category,
    languageShortcode: Option[String] = None,
    patternTokens: Option[List[PatternToken]] = None,
    pattern: Option[Pattern] = None,
    description: String,
    message: String,
    url: Option[String] = None,
    suggestions: List[TextSuggestion]
) extends CheckerRule {
  val replacement: Option[TextSuggestion] = None
}

object LTRule {
  def fromLT(lt: LanguageToolRule): LTRule = {
    val maybePatternTokens = lt match {
      case rule: LTPatternRule =>
        Some(
          rule.getPatternTokens.asScala.toList
            .map(PatternToken.fromLT)
        )
      case _ => None
    }

    val maybeLanguageShortcode = lt match {
      case rule: LTPatternRule => Some(rule.getLanguage.getShortCode)
      case _                   => None
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
    val patternTokens: JList[LTPatternToken] =
      rule.patternTokens.getOrElse(List[PatternToken]()).map(PatternToken.toLT).asJava
    val language = Languages.getLanguageForShortCode(rule.languageShortcode.getOrElse("en-GB"))
    val message = rule.suggestions match {
      case Nil => rule.message
      case suggestions =>
        val ruleMessage = if (rule.message == "") "" else rule.message + ". "
        ruleMessage.concat(
          suggestions.map(s => s"<suggestion>${s.text}</suggestion>").mkString(", ")
        )
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

  implicit val patternWrites: Writes[Pattern] = new Writes[Pattern] {
    def writes(pattern: Pattern) = JsString(pattern.toString)
  }
  implicit val patternReads: Reads[Pattern] = JsPath.read[String].map { regex =>
    Pattern.compile(regex)
  }

  implicit val writes: Writes[LTRule] = Json.writes[LTRule]
  implicit val reads: Reads[LTRule] = Json.reads[LTRule]
}

case class DictionaryRule(
    id: String,
    word: String,
    category: Category
) extends CheckerRule

object DictionaryRule {
  implicit val formats: OFormat[DictionaryRule] = Json.format[DictionaryRule]
}
