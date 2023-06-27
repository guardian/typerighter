package service

import play.api.Logging

import java.io._
import java.util.Collections
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.{Sheets, SheetsScopes}

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}
import db.{DbRuleDraft, Tag, Tags}

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
class SheetsRuleResource(credentialsJson: String, spreadsheetId: String) extends Logging {
  private val APPLICATION_NAME = "Typerighter"
  private val JSON_FACTORY = GsonFactory.getDefaultInstance()

  private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport
  private val credentials = getCredentials(credentialsJson)
  private val service = new Sheets.Builder(
    HTTP_TRANSPORT,
    JSON_FACTORY,
    credentials
  ).setApplicationName(APPLICATION_NAME).build

  private var availableTags = Tags.findAll()

  def getRules(): Either[List[String], List[DbRuleDraft]] = {
    getPatternRules()
  }

  /** Get rules that match using patterns, e.g. `RegexRule`, `LTRule`.
    */
  private def getPatternRules(): Either[List[String], List[DbRuleDraft]] = {
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

      if (errors.nonEmpty) {
        Left(errors)
      } else {
        Right(rules)
      }
    }
  }

  private def getRuleFromRow(row: List[Any], index: Int): Try[Option[DbRuleDraft]] = {
    try {
      val ruleType = row(PatternRuleCols.Type)
      val maybeIgnore = row.lift(PatternRuleCols.ShouldIgnore)
      val maybeId = row.lift(PatternRuleCols.Id).asInstanceOf[Option[String]]
      val rowNumber = index + 1
      val maybeRuleType = Map(
        "regex" -> "regex",
        "lt" -> "languageToolXML",
        "lt_core" -> "languageToolCore"
      ).get(ruleType.asInstanceOf[String])
      (maybeId, maybeIgnore, maybeRuleType) match {
        case (None, _, _) => Failure(new Exception(s"no id for rule (row: ${rowNumber})"))
        case (Some(id), _, _) if id.isEmpty =>
          Failure(new Exception(s"empty id for rule (row: ${rowNumber})"))
        case (Some(id), _, None) =>
          Failure(new Exception(s"Rule type ${ruleType} for rule with id ${id} not supported"))
        case (Some(_), None, _) =>
          Failure(new Exception(s"no Ignore column for rule (row: ${rowNumber})"))
        case (Some(id), Some(ignore), Some(ruleType)) =>
          val tagNames = cellToOptionalString(row, PatternRuleCols.Tags)
            .getOrElse("")
            .split(",")
            .toList
            .map(_.trim)
            .filter(_.nonEmpty)
          val tagsToAdd = tagNames
            .filter(tagName => !availableTags.exists(tag => tag.name == tagName))
            .map(name => Tag(None, name))
          if (tagsToAdd.size > 0) {
            Tags.batchInsert(tagsToAdd)
            availableTags = Tags.findAll()
          }
          val tagIds = tagNames.map(name => availableTags.find(tag => tag.name == name).get.id.get)
          Success(
            Some(
              DbRuleDraft.withUser(
                id = None,
                ruleType = ruleType,
                pattern = row.lift(PatternRuleCols.Pattern).asInstanceOf[Option[String]],
                replacement = cellToOptionalString(row, PatternRuleCols.Replacement),
                category = row.lift(PatternRuleCols.Category).asInstanceOf[Option[String]],
                tags = tagIds,
                description = row.lift(PatternRuleCols.Description).asInstanceOf[Option[String]],
                ignore = if (ignore.toString == "TRUE") true else false,
                notes = cellToOptionalString(row, PatternRuleCols.Notes),
                externalId = Some(id),
                forceRedRule = Some(
                  row.lift(PatternRuleCols.ForceRed).asInstanceOf[Option[String]].contains("y")
                ),
                advisoryRule = Some(
                  row.lift(PatternRuleCols.Advisory).asInstanceOf[Option[String]].contains("y")
                ),
                user = "Google Sheet"
              )
            )
          )
      }

    } catch {
      case e: Throwable => {
        Failure(new Exception(s"Error parsing rule at index ${index} – ${e.getMessage()}"))
      }
    }
  }

  private def cellToOptionalString(row: List[Any], col: Int): Option[String] =
    row(col).asInstanceOf[String] match {
      case "" => None
      case s  => Some(s)
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
      .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS)): @annotation.nowarn
  }
}
