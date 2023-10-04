package services

import com.gu.typerighter.model.TextRange
import opennlp.tools.namefind.{NameFinderME, TokenNameFinderModel}
import opennlp.tools.tokenize.SimpleTokenizer

import java.io.InputStream

case class EntityInText(word: String, range: TextRange, model: String)

class EntityHelper() {
  val personInputFile: InputStream =
    getClass.getClassLoader.getResourceAsStream("resources/opennlp/en-ner-person.bin")
  val personModel = new TokenNameFinderModel(personInputFile)
  val personFinder = new NameFinderME(personModel)

  val organizationInputFile: InputStream =
    getClass.getClassLoader.getResourceAsStream("resources/opennlp/en-ner-organization.bin")
  val organizationModel = new TokenNameFinderModel(organizationInputFile)
  val organizationFinder = new NameFinderME(organizationModel)

  val locationInputFile: InputStream =
    getClass.getClassLoader.getResourceAsStream("resources/opennlp/en-ner-location.bin")
  val locationModel = new TokenNameFinderModel(locationInputFile)
  val locationFinder = new NameFinderME(locationModel)

  def getAllEntitiesFromText(text: String): List[EntityInText] = {
    getEntitiesFromText(text, "person") ++
      getEntitiesFromText(text, "organization") ++
      getEntitiesFromText(text, "location")
  }

  def getEntitiesFromText(text: String, model: String): List[EntityInText] = {
    val finder = model match {
      case "person"       => personFinder
      case "organization" => organizationFinder
      case "location"     => locationFinder
      case _              => throw new Error("Model not found")
    }

    val tokenSpans = SimpleTokenizer.INSTANCE.tokenizePos(text)
    val entitySpans = finder.find(tokenSpans.map(_.getCoveredText(text).toString))

    entitySpans
      .flatMap(entitySpan =>
        (entitySpan.getStart until entitySpan.getEnd).map(tokenIndex => {
          val token = tokenSpans(tokenIndex)
          val from = token.getStart
          val to = token.getEnd

          EntityInText(
            word = text.slice(from, to),
            range = TextRange(
              from = from,
              to = to
            ),
            model = model
          )
        })
      )
      .toList
  }
}
