package rules

import java.io._
import java.util.Collections

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.{Sheets, SheetsScopes}
import model.{Category, PatternRule, PatternToken}

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
class SheetsRuleResource(credentialsJson: String, spreadsheetId: String, range: String) extends RuleResource {
  private val APPLICATION_NAME = "Typerighter"
  private val JSON_FACTORY = JacksonFactory.getDefaultInstance

  def fetchByCategory(): Future[(Map[Category, List[PatternRule]], List[String])] = { // Build a new authorized API client service.
    val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport
    val credentials = getCredentials(credentialsJson)
    val service = new Sheets.Builder(
      HTTP_TRANSPORT,
      JSON_FACTORY,
      credentials
    ).setApplicationName(APPLICATION_NAME).build
    val response = service.spreadsheets.values.get(spreadsheetId, range).execute
    val values = response.getValues
    if (values == null || values.isEmpty) {
      Future.failed(new Throwable("No rules found in sheet"))
    } else {
      val (rules, errors) = values.asScala.zipWithIndex.foldLeft((List.empty[PatternRule], List.empty[String])) {
        case ((rules, errors), (row, index)) => {
          getPatternRuleFromRow(row.asScala.toList, index) match {
            case Success(rule) => (rules :+ rule, errors)
            case Failure(error) => (rules, errors :+ error.getMessage)
          }
        }
      }
      Future.successful((rules.groupBy(_.category)), errors)
    }
  }

  private def getPatternRuleFromRow(row: List[AnyRef], index: Int): Try[PatternRule] = {
    try {
      val category = row(4).asInstanceOf[String]
      val colour = row(3).asInstanceOf[String]
      val rule = row(1).asInstanceOf[String]
      val description = row(6).asInstanceOf[String]
      val suggestion = row(2).asInstanceOf[String]
      // We split on whitespace here as LT expects separate words to be different tokens.
      val rules = rule.split(" ").toList.map(PatternToken(
        _,
        false,
        true,
        false
      ))

      Success(model.PatternRule(
        id = index.toString,
        category = Category(category, category, colour),
        languageShortcode = "en-GB",
        patternTokens = Some(rules),
        description = description,
        message = description,
        url = None,
        suggestions = if (suggestion.isEmpty) List.empty else List(suggestion)
      ))
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
