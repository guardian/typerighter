package rules

import java.io._
import java.util.Collections

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.{Sheets, SheetsScopes}
import model.{Category, RegexRule, TextSuggestion}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import model.{LTRule, BaseRule, RuleResource}
import scala.concurrent.ExecutionContext
import services.MatcherPool
import matchers.RegexMatcher
import matchers.LanguageToolFactory
import play.api.Logging

/**
  * A resource that gets rules from the given Google Sheet.
  *
  * @param credentialsJson A string containing the JSON the Google credentials service expects
  * @param spreadsheetId Available in the sheet URL
  */
class SheetsRuleManager(credentialsJson: String, spreadsheetId: String, matcherPool: MatcherPool, languageToolFactory: LanguageToolFactory) extends Logging {
  private val APPLICATION_NAME = "Typerighter"
  private val JSON_FACTORY = JacksonFactory.getDefaultInstance

  private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport
  private val credentials = getCredentials(credentialsJson)
  private val service = new Sheets.Builder(
    HTTP_TRANSPORT,
    JSON_FACTORY,
    credentials
  ).setApplicationName(APPLICATION_NAME).build

  def getRules()(implicit ec: ExecutionContext): Either[List[String], RuleResource] = {
    val maybeRegexRules = getPatternRules()
    val maybeLTRuleIds = getLanguageToolDefaultRuleIds()

    (maybeRegexRules, maybeLTRuleIds) match {
      case (Right(regexRules), Right(ltRules)) => Right(RuleResource(regexRules, ltRules))
      case (maybeRegex, maybeLt) => Left(maybeLTRuleIds.left.getOrElse(Nil) ++ maybeRegexRules.left.getOrElse(Nil))
    }
  }

  private def getPatternRules()(implicit ec: ExecutionContext): Either[List[String], List[BaseRule]] = {
    getRulesFromSheet("regexRules", "A:N", getRuleFromRow)
  }

  private def getLanguageToolDefaultRuleIds()(implicit ec: ExecutionContext): Either[List[String], List[String]] = {
    getRulesFromSheet("languagetoolRules", "A:C", getLTRuleFromRow)
  }

  private def getRulesFromSheet[RuleData](sheetName: String, sheetRange: String, rowToRule: (List[Object], Int) => Try[Option[RuleData]]): Either[List[String], List[RuleData]] = {
    val response = service
      .spreadsheets
      .values
      .get(spreadsheetId, s"$sheetName!$sheetRange")
      .execute
    val values = response.getValues
    if (values == null || values.isEmpty) {
      Left(List(s"Found no rules to ingest for sheet ${spreadsheetId}"))
    } else {
      val (rules, errors) = values
        .asScala
        .zipWithIndex
        .tail // The first row contains the table headings
        .foldLeft((List.empty[RuleData], List.empty[String])) {
          case ((rules, errors), (row, index)) => {
            rowToRule(row.asScala.toList, index) match {
              case Success(rule) => (rules ++ rule, errors)
              case Failure(error) => (rules, errors :+ error.getMessage)
            }
          }
        }

      if (errors.size != 0) {
        Left(errors)
      } else {
        Right(rules)
      }
    }
  }

  private def getRuleFromRow(row: List[Any], index: Int): Try[Option[BaseRule]] = {
    try {
      val ruleType = row(0)
      val maybeIgnore = row.lift(8)
      val maybeId = row.lift(10).asInstanceOf[Option[String]]

      (maybeId, maybeIgnore, ruleType) match {
        case (_, Some("TRUE"), _) => Success(None)
        case (None, _, _) => Failure(new Exception(s"no id for rule"))
        case (Some(id), _, _) if id.length == 0 => Failure(new Exception(s"empty id for rule"))
        case (Some(id), _, "regex") => Success(Some(getRegexRule(
            id,
            row(1).asInstanceOf[String],
            row(2).asInstanceOf[String],
            row(3).asInstanceOf[String],
            row(4).asInstanceOf[String],
            row.lift(6).asInstanceOf[Option[String]]
          )))
         case (Some(id), _, "lt") => Success(Some(getLTRule(
            id,
            row(1).asInstanceOf[String],
            row(2).asInstanceOf[String],
            row(3).asInstanceOf[String],
            row(4).asInstanceOf[String],
            row.lift(6).asInstanceOf[Option[String]]
          )))
      }

    } catch {
      case e: Throwable => {
        Failure(new Exception(s"Error parsing rule at index ${index} – ${e.getMessage()}"))
      }
    }
  }

  private def getRegexRule(
    id: String,
    pattern: String,
    suggestion: String,
    colour: String,
    category: String,
    description: Option[String]
  ) = {
    RegexRule(
      id = id,
      category = Category(category, category, colour),
      description = description.getOrElse(""),
      replacement = if (suggestion.isEmpty) None else Some(TextSuggestion(suggestion)),
      regex = pattern.r,
    )
  }

  private def getLTRule(
    id: String,
    pattern: String,
    suggestion: String,
    colour: String,
    category: String,
    description: Option[String]
  ) = {
    LTRule(
      id = id,
      category = Category(category, category, colour),
      description = description.getOrElse(""),
      message = description.getOrElse("No description"),
      suggestions = Nil
    )
  }

  private def getLTRuleFromRow(row: List[Any], index: Int): Try[Option[String]] = {
     try {
      val shouldInclude = row.lift(0)
      shouldInclude match {
        case Some("Y") => {
          val ruleId = row(1).asInstanceOf[String]
          Success(Some(ruleId))
        }
        case _ => Success(None)
      }

    } catch {
      case e: Throwable => Failure(new Exception(s"Error parsing rule at index ${index} -- ${e.getMessage}"))
    }
  }

  /**
    * Creates an authorized Credential object.
    *
    * @param credentialsJson A string containing the JSON the Google credentials service expects
    * @return An authorized Credential object
    */
  private def getCredentials(credentialsJson: String) = {
    val in = new ByteArrayInputStream(credentialsJson.getBytes)
    GoogleCredential
      .fromStream(in)
      .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS))
  }
}
