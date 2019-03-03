package services

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import java.io._
import java.util.Collections

import scala.collection.JavaConversions._
import model.{Category, PatternRule, PatternToken}
import play.api.{Configuration, Logger}



object SheetsResource {
  private val APPLICATION_NAME = "Google Sheets API Java Quickstart"
  private val JSON_FACTORY = JacksonFactory.getDefaultInstance
  private val TOKENS_DIRECTORY_PATH = "tokens"
  /**
    * Global instance of the scopes required by this quickstart.
    * If modifying these scopes, delete your previously saved tokens/ folder.
    */
  private val SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY)
  private val CREDENTIALS_FILE_PATH = "credentials.json"

  /**
    * Creates an authorized Credential object.
    *
    * @param HTTP_TRANSPORT The network HTTP Transport.
    * @return An authorized Credential object.
    * @throws IOException If the credentials.json file cannot be found.
    */
  private def getCredentials(configuration: Configuration, HTTP_TRANSPORT: NetHttpTransport) = {
    // Load client secrets.
    val in = new FileInputStream(CREDENTIALS_FILE_PATH)
    val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in))

    // Build flow and trigger user authorization request.
    val flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH))).setAccessType("offline").build
    val receiver = new LocalServerReceiver.Builder().setPort(8000).build
    new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
  }

  /**
    * Prints the names and majors of students in a sample spreadsheet:
    * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
    */
  def getDictionariesFromSheet(configuration: Configuration): Option[List[PatternRule]] = { // Build a new authorized API client service.
    val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport
    val result = for {
      spreadsheetId <- configuration.getOptional[String]("typerighter.sheetId")
      range <- configuration.getOptional[String]("typerighter.sheetRange")
    } yield {
      val service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(configuration, HTTP_TRANSPORT)).setApplicationName(APPLICATION_NAME).build
      val response = service.spreadsheets.values.get(spreadsheetId, range).execute
      val values = response.getValues
      if (values == null || values.isEmpty) {
        None
      } else {
        val patternRules = values.toList.zipWithIndex.flatMap {
          case (row, index) => getPatternRuleFromRow(row.toList, index)
          case _ => None
        }
        Some(patternRules)
      }
    }
    result.flatten
  }

  private def getPatternRuleFromRow(row: List[AnyRef], index: Int): Option[PatternRule] = {
    try {
      val category = row.get(3).asInstanceOf[String]
      val rule = row.get(1).asInstanceOf[String]
      val description = row.get(5).asInstanceOf[String]
      Some(PatternRule(
        id = index.toString,
        category = Category("TYPOS", "Possible Typo"),
        languageShortcode = "en-GB",
        patternTokens = Some(List(PatternToken(
          rule,
          false,
          true,
          false
        ))),
        description = description,
        message = description,
        url = None
      ))
    } catch {
      case e: Exception =>
        Logger.error(s"Error parsing sheet row: ${e}")
        None
    }
  }
}
