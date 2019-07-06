package services

import java.util.UUID

import model.{Category, ResponseRule, RuleMatch}
import utils.Validator

import scala.concurrent.Future

class NameCheckerValidator(nameFinder: NameFinder, wikiNameSearcher: WikiNameSearcher) extends Validator {
  val responseRule =
  def getCategory = "Name"
  def getRules = List.empty
  def check(request: ValidatorRequest): List[RuleMatch] = {
    val names = nameFinder.findNames(request.text)
    val results = names.map { name => wikiNameSearcher.fetchWikiMatchesForName(name.text).map {
      result => {
        val firstSearchResult = result.query.search.headOption.map { searchResult =>
          searchResult.snippet
        }
        RuleMatch(
          getResponseRule,
          name.from,
          name.to,
          message = firstSearchResult.getOrElse("No result")
        )
      }
    }}
    Future.sequence(results)
  }
  private def getResponseRule = ResponseRule(
    UUID.randomUUID().toString,
    "Name check description",
    Category("name-check", "Name check", "teal"),
    "")
}
