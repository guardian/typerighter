package services

import java.util.UUID

import model.{Category, ResponseRule, RuleMatch}
import utils.Validator

import scala.concurrent.{ExecutionContext, Future}
import model.WikiSuggestion
import model.WikiAbstract

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
      val ruleMatch = RuleMatch(
        getResponseRule,
        name.from,
        name.to,
        message = message,
        shortMessage = Some(message),
        suggestedReplacements = None
      )
      wikiNameSearcher.fetchWikiMatchesForName(name.text).map {
        case Some(result) => {
          val matches = result.hits.hits.map { hit =>
            WikiAbstract(hit._source.title.mkString, hit._source.`abstract`.mkString, "", hit._score)
          }
          ruleMatch.copy(suggestedReplacements = Some(WikiSuggestion(matches)))
        }
        case None => ruleMatch
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
