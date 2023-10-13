package services

import com.gu.typerighter.model.TextRange
import opennlp.tools.namefind.{NameFinderME, TokenNameFinderModel}
import opennlp.tools.tokenize.SimpleTokenizer

import java.io.InputStream

case class EntityInText(word: String, range: TextRange)

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

  val allFinders: List[NameFinderME] = List(personFinder, organizationFinder, locationFinder)

  def getEntitiesFromText(text: String, offset: Int = 0): List[EntityInText] = {
    val tokenSpans = SimpleTokenizer.INSTANCE.tokenizePos(text)

    allFinders.foldLeft(List.empty[EntityInText])((entities, finder) => {
      val entitySpans = finder.find(tokenSpans.map(_.getCoveredText(text).toString))

      entities ++ entitySpans
        .flatMap(entitySpan =>
          (entitySpan.getStart until entitySpan.getEnd).map(tokenIndex => {
            val token = tokenSpans(tokenIndex)
            val from = token.getStart
            val to = token.getEnd

            EntityInText(
              word = text.slice(from, to),
              range = TextRange(
                from = from + offset,
                to = to + offset
              )
            )
          })
        )
        .toList
    })
  }
}
