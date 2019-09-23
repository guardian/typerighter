package services

import java.util.UUID

import model.{Category, ResponseRule, RuleMatch}
import utils.{NameCheckerUtils, Validator}

import scala.concurrent.{ExecutionContext, Future}
import model.WikiSuggestion

class NameCheckerValidator(
    nameFinder: StanfordNameFinder,
    wikiNameSearcher: WikiNameSearcher
)(implicit ec: ExecutionContext)
    extends Validator {
  private val category = Category("name-check", "Name check", "teal")
  def getCategory = category.id
  def getId = "name-checker"
  def getRules = List.empty
  def check(request: ValidatorRequest): Future[List[RuleMatch]] = {
    println(s"Checking names for blocks ${request.blocks.map(_.id).mkString(", ")}")
    val namesAndBlocks = request.blocks.map { block => (block, nameFinder.findNames(block.text)) }
    val allNames = namesAndBlocks.flatMap { case (_, names) => names }
    val results = for {
      (block, names) <- namesAndBlocks
      name <- names
    } yield {
      val message = s"Name found: ${name.text}"
      val normalisedName = getNormalisedName(name.text, allNames)
      println(s"Got name -- ${name.text}, $normalisedName")
      wikiNameSearcher.fetchWikiMatchesForName(normalisedName).map { nameResult =>
        val matches = nameResult.results.map { result =>
          WikiSuggestion(result.name, result.title, result.score)
        }
        RuleMatch(
          getResponseRule,
          name.from + block.from,
          name.to + block.from,
          message = message,
          shortMessage = Some(message),
          suggestions = matches
        )
      }
    }

    Future.sequence(results)
  }

  private def getNormalisedName(nameStr: String, otherNames: List[NameResult]) = {
    NameCheckerUtils.findSimilarLastNames(nameStr, otherNames.map { _.text } ) match {
      case Nil => nameStr
      case similarNames: List[String] =>
        println(s"These names are similar to $nameStr: ${similarNames.mkString(", ")}")
        similarNames.head
    }
  }

  private def getResponseRule =
    ResponseRule(
      UUID.randomUUID().toString,
      "Name check description",
      category,
      ""
    )
}
