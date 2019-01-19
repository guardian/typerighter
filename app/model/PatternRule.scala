package model

import java.util.{List => JList}

import org.languagetool.Languages
import org.languagetool.rules.patterns.{PatternToken => LTPatternToken}
import org.languagetool.rules.patterns.{PatternRule => LTPatternRule}
import play.api.libs.json.{Json, Reads, Writes}

import scala.collection.JavaConverters._

/**
  * The application's representation of a LanguageTool PatternRule.
  */
case class PatternRule(id: String,
                       category: Category,
                       languageShortcode: String,
                       patternTokens: Option[List[PatternToken]],
                       description: String,
                       message:String,
                       url: Option[String])

object PatternRule {
  def fromLT(lt: LTPatternRule): PatternRule = {
    val patternTokens = Some(lt
      .getPatternTokens
      .asScala
      .toList
      .map(pt => PatternToken.fromLT(pt)))

    PatternRule(
      id = lt.getId,
      category = Category.fromLT(lt.getCategory),
      languageShortcode = lt.getLanguage.getShortCode,
      patternTokens = patternTokens,
      description = lt.getDescription,
      message = lt.getMessage,
      url = Option(if (lt.getUrl == null) null else lt.toString)
    )
  }

  def toLT(rule: PatternRule): LTPatternRule = {
    val patternTokens: JList[LTPatternToken] = seqAsJavaList(rule.patternTokens.getOrElse(List[PatternToken]()).map(PatternToken.toLT))
    val language = Languages.getLanguageForShortCode(rule.languageShortcode)
    val ltPatternRule = new LTPatternRule(
      rule.id,
      language,
      patternTokens,
      rule.description,
      rule.message,
      rule.message
    )
    ltPatternRule.setCategory(Category.toLT(rule.category))
    ltPatternRule
  }

  implicit val writes: Writes[PatternRule] = Json.writes[PatternRule]

  implicit val reads: Reads[PatternRule] = Json.reads[PatternRule]
}
