package services

import com.google.api.client.googleapis.auth.oauth2.{GoogleCredential}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import java.io._
import java.util.Collections

import scala.collection.JavaConverters._
import model.{Category, PatternRule, PatternToken}
import play.api.Configuration

import scala.util.{Failure, Success, Try}

object SheetsRuleResource {
  private val APPLICATION_NAME = "Google Sheets API Java Quickstart"
  private val JSON_FACTORY = JacksonFactory.getDefaultInstance
  private val TOKENS_DIRECTORY_PATH = "tokens"

  /**
    * Global instance of the scopes required by this quickstart.
    * If modifying these scopes, delete your previously saved tokens/ folder.
    */
  private val SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY)

  /**
    * Creates an authorized Credential object.
    *
    * @param credentialsFilePath A string containing the credentials file path.
    *                            These can be generated in Google Platform at
    *                            https://console.cloud.google.com/apis/credentials.
    * @return An authorized Credential object.
    * @throws IOException If the credentials.json file cannot be found.
    */
  private def getCredentials(credentials: String) = {
    val in = new ByteArrayInputStream(credentials.getBytes)
    GoogleCredential
      .fromStream(in)
      .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS))
  }

  def getDictionariesFromSheet(configuration: Configuration): (List[PatternRule], List[String]) = { // Build a new authorized API client service.
    val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport
    val maybeResult = for {
      credentialsData <- configuration.getOptional[String]("typerighter.google.credentials")
      spreadsheetId <- configuration.getOptional[String]("typerighter.sheetId")
      range <- configuration.getOptional[String]("typerighter.sheetRange")
    } yield {
      val credentials = getCredentials(credentialsData)
      val service = new Sheets.Builder(
        HTTP_TRANSPORT,
        JSON_FACTORY,
        credentials
      ).setApplicationName(APPLICATION_NAME).build
      val response = service.spreadsheets.values.get(spreadsheetId, range).execute
      val values = response.getValues
      if (values == null || values.isEmpty) {
        (Nil, Nil)
      } else {
        values.asScala.zipWithIndex.foldLeft((List.empty[PatternRule], List.empty[String])) {
          case ((rules, errors), (row, index)) => {
            getPatternRuleFromRow(row.asScala.toList, index) match {
              case Success(rule) => (rules :+ rule, errors)
              case Failure(error) => (rules, errors :+ error.getMessage)
            }
          }
        }
      }
    }
    maybeResult.getOrElse((Nil, Nil))
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

      Success(PatternRule(
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
}
