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

/**
  * A resource that fetches rules from the given Google Sheet.
  *
  * @param credentialsJson A string containing the JSON the Google credentials service expects
  * @param spreadsheetId Available in the sheet URL
  * @param range E.g. "A:G"
  */
class SheetsRuleResource(credentialsJson: String, spreadsheetId: String) {
  private val APPLICATION_NAME = "Typerighter"
  private val JSON_FACTORY = JacksonFactory.getDefaultInstance

  def fetchRulesByCategory(): Future[(Map[Category, List[RegexRule]], List[String])] = { // Build a new authorized API client service.
    val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport
    val credentials = getCredentials(credentialsJson)
    val service = new Sheets.Builder(
      HTTP_TRANSPORT,
      JSON_FACTORY,
      credentials
    ).setApplicationName(APPLICATION_NAME).build
    val response = service.spreadsheets.values.get(spreadsheetId, "A:N").execute
    val values = response.getValues
    if (values == null || values.isEmpty) {
      Future.successful((Map(), Nil))
    } else {
      val (rules, errors) = values
        .asScala
        .zipWithIndex
        .tail // The first row contains the table headings
        .foldLeft((List.empty[RegexRule], List.empty[String])) {
        case ((rules, errors), (row, index)) => {
          getPatternRuleFromRow(row.asScala.toList, index) match {
            case Success(rule) => (rules ++ rule, errors)
            case Failure(error) => (rules, errors :+ error.getMessage)
          }
        }
      }
      Future.successful((rules.groupBy(_.category), errors))
    }
  }

  private def getPatternRuleFromRow(row: List[Any], index: Int): Try[Option[RegexRule]] = {
    try {
      val shouldIgnore = row.lift(8)

      shouldIgnore match {
        case Some("TRUE") => Success(None)
        case _ => {
          val rule = row(1).asInstanceOf[String]
          val suggestion = row(2).asInstanceOf[String]
          val colour = row(3).asInstanceOf[String]
          val category = row(4).asInstanceOf[String]
          val description = row.lift(6).asInstanceOf[Option[String]]
          val maybeId = row.lift(10).asInstanceOf[Option[String]]

          maybeId match {
            case Some(id) => Success(Some(RegexRule(
              id = id,
              category = Category(category, category, colour),
              description = description.getOrElse(""),
              replacement = if (suggestion.isEmpty) None else Some(TextSuggestion(suggestion)),
              regex = rule.r,
            )))
            case None => {
              throw new Exception(s"No id for rule ${rule}")
            }
          }
        }
      }
    } catch {
      case e: Throwable => {
        Failure(new Exception(s"Error parsing rule at index ${index} – ${e.getMessage()}"))
      }
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
