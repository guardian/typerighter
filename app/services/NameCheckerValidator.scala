package services

import java.util.UUID

import model.{Category, ResponseRule, RuleMatch}
import utils.Validator

import scala.concurrent.{ExecutionContext, Future}
import model.WikiSuggestion

class NameCheckerValidator(
    nameFinder: NameFinder,
    wikiNameSearcher: WikiNameSearcher
)(implicit ec: ExecutionContext)
    extends Validator {
  def getCategory = "Name"
  def getRules = List.empty
  def check(request: ValidatorRequest): Future[List[RuleMatch]] = {
    val names = nameFinder.findNames(request.text)
    val results = names.map { name =>
      val message = s"Name found: ${name.text}"
      wikiNameSearcher.fetchWikiMatchesForName(name.text).map { nameResult =>
        val matches = nameResult.results.map { result =>
          WikiSuggestion(result.name, result.title, result.score)
        }
        RuleMatch(
          getResponseRule,
          name.from,
          name.to,
          message = message,
          shortMessage = Some(message),
          suggestions = matches
        )
      }
    }
    Future.sequence(results)
  }
  private def getResponseRule =
    ResponseRule(
      UUID.randomUUID().toString,
      "Name check description",
      Category("name-check", "Name check", "teal"),
      ""
    )
}
