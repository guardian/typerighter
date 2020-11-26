package model

import java.util.{List => JList}
import java.util.regex.Pattern

import play.api.libs.json.{JsObject, JsPath, JsResult, JsString, JsSuccess, Json, Reads, Writes}
import play.api.libs.json._
import play.api.libs.json.Reads._
import org.languagetool.Languages
import org.languagetool.rules.patterns.{PatternRule => LTPatternRule, PatternToken => LTPatternToken}
import org.languagetool.rules.{Rule => LanguageToolRule}

import scala.util.matching.Regex
import scala.collection.JavaConverters._
import utils.Text
import matchers.RegexMatcher
import model.PronounGroup.PronounGroup
import model.PronounType.PronounType

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

object PronounType extends Enumeration {
  type PronounType = Value
  val SingularSubject, SingularObject, PossessivePronoun, PossessiveAdjective, Unknown = Value
}
object PronounGroup extends Enumeration {
  type PronounGroup = Value
  val Personal, Possessive, Unknown = Value
}

sealed trait Pronoun {
  val id: String

  val singularSubject: String
  val singularObject: String
  val possessivePronoun: String
  val possessiveAdjective: String

  def stringMatchesPronoun(text: String): Boolean = {
    val t = text.toLowerCase()

    (t == this.singularObject || t == this.singularSubject || t == this.possessiveAdjective || t == this.possessivePronoun)
  }

  // TODO: Pass the tags into here and use to determine between her and her
  def getPronounType(text: String, pronounGroup: PronounGroup): PronounType

  def getValueByPronounType(pronounType: PronounType): Option[String] = {
    pronounType match {
      case PronounType.SingularSubject => Some(this.singularSubject)
      case PronounType.SingularObject => Some(this.singularObject)
      case PronounType.PossessivePronoun => Some(this.possessivePronoun)
      case PronounType.PossessiveAdjective => Some(this.possessiveAdjective)
      case PronounType.Unknown => None
    }
  }
}

object Pronoun {
  implicit val writes: Writes[Pronoun] = (pronoun: Pronoun) => JsString(pronoun.id)
  implicit val reads: Reads[Pronoun] = (JsPath).read[String].map {
    case "HE_HIS" => HE_HIS
    case "SHE_HERS" => SHE_HERS
    case "THEY_THEM" => THEY_THEM
    case _ => UNKNOWN
  }

  def getPronounFromString(text: String): Pronoun = {
    if (HE_HIS.stringMatchesPronoun(text)) {
      HE_HIS
    } else if (SHE_HERS.stringMatchesPronoun(text)) {
      SHE_HERS
    } else if (THEY_THEM.stringMatchesPronoun(text)) {
      THEY_THEM
    } else {
      UNKNOWN
    }
  }
}

// he, him, his
case object HE_HIS extends Pronoun {
  val id = "HE_HIS"

  val singularSubject = "he"
  val singularObject = "him"
  val possessivePronoun = "his"
  val possessiveAdjective = "his"

  def getPronounType(text: String, pronounGroup: PronounGroup): PronounType = {
    text match {
      case "he"   => PronounType.SingularSubject
      case "him"   => PronounType.SingularObject
      case "his"  => PronounType.Unknown
      case _       => PronounType.Unknown
    }
  }

  override def toString: String = "he/him/his"
}

// she, her, hers
case object SHE_HERS extends Pronoun {
  val id = "SHE_HERS"

  val singularSubject = "she"
  val singularObject = "her"
  val possessivePronoun = "her"
  val possessiveAdjective = "hers"

  def getPronounType(text: String, pronounGroup: PronounGroup): PronounType = {
    text match {
      case "she"   => PronounType.SingularSubject
      case "her"   => if (pronounGroup == PronounGroup.Possessive) PronounType.PossessivePronoun else PronounType.SingularObject
      case "hers"  => PronounType.PossessiveAdjective
      case _       => PronounType.Unknown
    }
  }

  override def toString: String = "she/her/hers"
}

// they, them, their, theirs
case object THEY_THEM extends Pronoun {
  val id = "THEY_THEM"

  val singularSubject = "they"
  val singularObject = "them"
  val possessivePronoun = "their"
  val possessiveAdjective = "theirs"

  def getPronounType(text: String, pronounGroup: PronounGroup): PronounType = {
    text match {
      case "they"   => PronounType.SingularSubject
      case "them"   => PronounType.SingularObject
      case "their"  => PronounType.PossessivePronoun
      case "theirs" => PronounType.PossessiveAdjective
      case _        => PronounType.Unknown
    }
  }

  override def toString: String = "they/them/their/theirs"
}

case object UNKNOWN extends Pronoun {
  val id = "UNKNOWN"

  val singularSubject = "unknown"
  val singularObject = "unknown"
  val possessivePronoun = "unknown"
  val possessiveAdjective = "unknown"

  def getPronounType(text: String, pronounGroup: PronounGroup): PronounType = PronounType.Unknown

  override def getValueByPronounType(pronounType: PronounType): Option[String] = None

  override def toString: String = "unknown"
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
  val fullName: String = firstName + " " + lastName

  // TODO: This could probably be done more intelligently, maybe with a regex?
  val nameListForChecking: List[String] = List(
    firstName,
    lastName,
    fullName,
    addPossessiveApostrophe(firstName),
    addPossessiveApostrophe(lastName),
    addPossessiveApostrophe(fullName),
  )

  // TODO: The space added here is a hack to cope with the fact that mention in the chain has a space in between the
  //       name and 's
  private def addPossessiveApostrophe(name: String): String = {
    if (name.endsWith("s")) {
      s"$name '"
    } else {
      s"$name 's"
    }
  }
}
