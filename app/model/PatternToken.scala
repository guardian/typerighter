package model

import org.languagetool.rules.patterns.{PatternToken => LTPatternToken}
import play.api.libs.json.{Json, Reads, Writes}

/**
  * The application's representation of a LanguageTool PatternToken.
  */
case class PatternToken(
                       token: String,
                       caseSensitive: Boolean,
                       regexp: Boolean,
                       inflected: Boolean
                       )

object PatternToken {
  def fromLT(patternToken: LTPatternToken): PatternToken =
    PatternToken(
      token = patternToken.getString,
      caseSensitive = patternToken.isCaseSensitive,
      regexp = patternToken.isRegularExpression,
      inflected = patternToken.isInflected
    )

  def toLT(patternToken: PatternToken) =
    new LTPatternToken(
      patternToken.token,
      patternToken.caseSensitive,
      patternToken.regexp,
      patternToken.inflected)

  implicit val writes: Writes[PatternToken] = Json.writes[PatternToken]

  implicit val reads: Reads[PatternToken] = Json.reads[PatternToken]
}
