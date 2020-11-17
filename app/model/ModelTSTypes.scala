package model

import com.scalatsi._
import com.scalatsi.dsl._
import scala.util.matching.Regex
import java.util.regex.Pattern

object ModelTSTypes extends DefaultTSTypes {
  implicit val regex = TSType.sameAs[Regex, String]
  implicit val jsRegex = TSType.sameAs[Pattern, String]
  implicit val patternToken = TSType.fromCaseClass[PatternToken]
  implicit val ruleMatch: TSType[RuleMatch] = TSType.fromCaseClass[RuleMatch]
}
