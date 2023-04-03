package service

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.{Sheets, SheetsScopes}
import com.gu.typerighter.model._
import db.DbRule
import play.api.Logging

import java.io._
import java.util.Collections
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object PatternRuleCols {
  val Type = 0
  val Pattern = 1
  val Replacement = 2
  val Colour = 3
  val Category = 4
  val Description = 6
  val Tags = 7
  val ShouldIgnore = 8
  val Notes = 9
  val Id = 10
  val ForceRed = 11
  val Advisory = 13
}

/** A resource that gets rules from the given Google Sheet.
  *
  * @param credentialsJson
  *   A string containing the JSON the Google credentials service expects
  * @param spreadsheetId
  *   Available in the sheet URL
  */
class SheetsRuleManager(credentialsJson: String, spreadsheetId: String) extends Logging {
  private val APPLICATION_NAME = "Typerighter"
  private val JSON_FACTORY = GsonFactory.getDefaultInstance()

  private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport
  private val credentials = getCredentials(credentialsJson)
  private val service = new Sheets.Builder(
    HTTP_TRANSPORT,
    JSON_FACTORY,
    credentials
  ).setApplicationName(APPLICATION_NAME).build

  def getRules()(implicit ec: ExecutionContext): Either[List[String], List[DbRule]] = {
    getPatternRules()
  }

  /** Get rules that match using patterns, e.g. `RegexRule`, `LTRule`.
    */
  private def getPatternRules()(implicit
      ec: ExecutionContext
  ): Either[List[String], List[DbRule]] = {
    getRulesFromSheet("regexRules", "A:N", getRuleFromRow)
  }

  private def getRulesFromSheet[RuleData](
      sheetName: String,
      sheetRange: String,
      rowToRule: (List[Object], Int) => Try[Option[RuleData]]
  ): Either[List[String], List[RuleData]] = {
    val response = service.spreadsheets.values
      .get(spreadsheetId, s"$sheetName!$sheetRange")
      .execute
    val values = response.getValues
    if (values == null || values.isEmpty) {
      Left(List(s"Found no rules to ingest for sheet ${spreadsheetId}"))
    } else {
      val (rules, errors) =
        values.asScala.zipWithIndex.tail // The first row contains the table headings
          .foldLeft((List.empty[RuleData], List.empty[String])) {
            case ((rules, errors), (row, index)) => {
              rowToRule(row.asScala.toList, index) match {
                case Success(rule)  => (rules ++ rule, errors)
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

  private def getRuleFromRow(row: List[Any], index: Int): Try[Option[DbRule]] = {
    try {
      val ruleType = row(PatternRuleCols.Type)
      val maybeIgnore = row.lift(PatternRuleCols.ShouldIgnore)
      val maybeId = row.lift(PatternRuleCols.Id).asInstanceOf[Option[String]]
      val rowNumber = index + 1

      (maybeId, maybeIgnore, ruleType) match {
        case (_, Some("TRUE"), _) => Success(None)
        case (None, _, _)         => Failure(new Exception(s"no id for rule (row: ${rowNumber})"))
        case (Some(id), _, _) if id.isEmpty =>
          Failure(new Exception(s"empty id for rule (row: ${rowNumber})"))
        case (Some(id), _, ruleType) if !Set("regex", "lt", "lt_core").contains(ruleType.toString) =>
          Failure(new Exception(s"Rule type ${ruleType} for rule with id ${id} not supported"))
        case (Some(id), Some(ignore), ruleType) => Success(Some(DbRule(
          id = None,
          ruleType = ruleType.asInstanceOf[String],
          pattern = row.lift(PatternRuleCols.Pattern).asInstanceOf[Option[String]],
          replacement = row.lift(PatternRuleCols.Replacement).asInstanceOf[Option[String]],
          category = row.lift(PatternRuleCols.Category).asInstanceOf[Option[String]],
          tags = row.lift(PatternRuleCols.Tags).asInstanceOf[Option[String]],
          description = row.lift(PatternRuleCols.Description).asInstanceOf[Option[String]],
          ignore = if (ignore.toString == "TRUE") true else false,
          notes = row.lift(PatternRuleCols.Replacement).asInstanceOf[Option[String]],
          googleSheetId = Some(id),
          forceRedRule = Some(row.lift(PatternRuleCols.ForceRed).asInstanceOf[Option[String]].contains("y")),
          advisoryRule = Some(row.lift(PatternRuleCols.Advisory).asInstanceOf[Option[String]].contains("y"))
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
      category: String,
      description: Option[String]
  ) = RegexRule(
    id = id,
    category = Category(category, category),
    description = description.getOrElse(""),
    replacement = if (suggestion.isEmpty) None else Some(TextSuggestion(suggestion)),
    regex = new ComparableRegex(pattern)
  )

  private def getLTRuleXML(
      id: String,
      pattern: String,
      category: String,
      description: String
  ) = {
    LTRuleXML(id, pattern, Category(category, category), description)
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
      case e: Throwable =>
        Failure(new Exception(s"Error parsing rule at index ${index} -- ${e.getMessage}"))
    }
  }

  /** Creates an authorized Credential object.
    *
    * @param credentialsJson
    *   A string containing the JSON the Google credentials service expects
    * @return
    *   An authorized Credential object
    */
  private def getCredentials(credentialsJson: String) = {
    val in = new ByteArrayInputStream(credentialsJson.getBytes)
    GoogleCredential
      .fromStream(in)
      .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS))
  }
}
