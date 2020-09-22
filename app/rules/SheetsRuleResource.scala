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
import model.LTRule
import model.BaseRule
import scala.concurrent.ExecutionContext
import services.MatcherPool
import matchers.RegexMatcher
import matchers.LanguageToolFactory
import play.api.Logging

/**
  * A resource that fetches rules from the given Google Sheet.
  *
  * @param credentialsJson A string containing the JSON the Google credentials service expects
  * @param spreadsheetId Available in the sheet URL
  */
class SheetsRuleResource(credentialsJson: String, spreadsheetId: String, matcherPool: MatcherPool, languageToolFactory: LanguageToolFactory) extends Logging {
  private val APPLICATION_NAME = "Typerighter"
  private val JSON_FACTORY = JacksonFactory.getDefaultInstance

  private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport
  private val credentials = getCredentials(credentialsJson)
  private val service = new Sheets.Builder(
    HTTP_TRANSPORT,
    JSON_FACTORY,
    credentials
  ).setApplicationName(APPLICATION_NAME).build

  def addRulesToMatcherPool()(implicit ec: ExecutionContext): Future[(Int, List[String])] = {
    for {
      maybeRegexRules <- fetchRegexRules()
      maybeLTRules <- fetchLanguageToolDefaults()
    } yield {
      val (regexErrors, noOfRegexRulesIngested) = maybeRegexRules match {
        case Left(errors) => {
          logger.error(s"Could not parse regex rules from spreadsheet on init: ${errors.mkString}")
          (errors, 0)
        }
        case Right(rules) => {
          rules.groupBy(_.category).foreach { case (category, rules) => {
            val matcher = new RegexMatcher(category.name, rules)
            matcherPool.addMatcher(category, matcher)
          }}
          (Nil, rules.size)
        }
      }


      val (ltErrors, noOfLtRulesIngested) = maybeLTRules match {
        case Left(errors) => {
          logger.error(s"Could not parse LT defaultrules from spreadsheet on init: ${errors.mkString}")
          (errors, 0)
        }
        case Right(rules) => {
          val (matcher, errors) = languageToolFactory.createInstance("languagetool-default", Nil, rules)
          matcherPool.addMatcher(new Category(matcher.getCategory(), "Default LanguageTool rules", "puce"), matcher)
          (Nil, rules.size)
        }
      }

      (noOfRegexRulesIngested + noOfLtRulesIngested, regexErrors ++ ltErrors)
    }
  }

  private def fetchRegexRules()(implicit ec: ExecutionContext): Future[Either[List[String], List[RegexRule]]] = {
    fetchRulesFromSheet("regexRules", "A:N", getRegexRuleFromRow)
  }

  private def fetchLanguageToolDefaults()(implicit ec: ExecutionContext): Future[Either[List[String], List[String]]] = {
    fetchRulesFromSheet("languagetoolRules", "A:C", getLTRuleFromRow)
  }

  private def fetchRulesFromSheet[RuleData](sheetName: String, sheetRange: String, rowToRule: (List[Object], Int) => Try[Option[RuleData]]): Future[Either[List[String], List[RuleData]]] = {
    val response = service
      .spreadsheets
      .values
      .get(spreadsheetId, s"$sheetName!$sheetRange")
      .execute
    val values = response.getValues
    if (values == null || values.isEmpty) {
      Future.successful(Left(List(s"Found no rules to ingest for sheet ${spreadsheetId}")))
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
        Future.successful(Left(errors))
      } else {
        Future.successful(Right(rules))
      }
    }
  }

  private def getRegexRuleFromRow(row: List[Any], index: Int): Try[Option[RegexRule]] = {
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
