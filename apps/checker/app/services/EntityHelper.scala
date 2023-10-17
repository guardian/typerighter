package services

import com.gu.typerighter.model.TextRange
import opennlp.tools.namefind.{NameFinderME, TokenNameFinderModel}
import opennlp.tools.tokenize.SimpleTokenizer
import play.api.libs.json.{JsError, JsSuccess}

import java.io.InputStream
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.{Json, Reads}

case class EntityInText(word: String, range: TextRange)

class EntityHelper(wsClient: WSClient) {
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

  case class NERResult(label: String, text: String, start: Int, end: Int)

  object NERResult {
    implicit val reads: Reads[NERResult] = Json.reads[NERResult]
  }

  def getEntityResultFromNERService(text: String): Future[List[String]] = {
    val url = "https://ner.gutools.co.uk/v1/process"
    val key = "abc"
    val body = s"{\"articles\": [{\"text\": \"$text\"}],\"model\": \"en_core_web_trf\",\"entities\": [\"PERSON\", \"NORP\", \"FAC\",  \"LOC\", \"GPE\", \"PRODUCT\", \"EVENT\", \"WORK_OF_ART\"]}"

    wsClient.url(url)
      .withHttpHeaders(("API-KEY", key), ("accept", "application/json"), ("Content-Type", "application/json"))
      .post(body).map { response =>
      val entitiesJson = response.json.result("result")(0)("ents")
      entitiesJson.validate[List[NERResult]] match {
        case JsSuccess(value, _) =>
          value.map(_.text)
        case JsError(error) =>
          Nil
      }
    }
  }

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
