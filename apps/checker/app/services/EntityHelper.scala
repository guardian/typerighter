package services

import com.gu.typerighter.model.TextRange
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}
import play.api.libs.ws.WSClient
import utils.CheckerConfig

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class EntityInText(word: String, range: TextRange)

class EntityHelper(wsClient: WSClient, config: CheckerConfig) {
  case class NERResult(label: String, text: String, start: Int, end: Int)

  object NERResult {
    implicit val reads: Reads[NERResult] = Json.reads[NERResult]
  }

  val logger: Logger = Logger(getClass)

  def getEntityResultFromNERService(
      text: String,
      offset: Int = 0
  ): Future[Either[Error, List[EntityInText]]] = {
    val url = config.nerApiUrl
    val key = config.nerApiKey
    val model = "en_core_web_trf"
    val entityTypes =
      List("PERSON", "NORP", "FAC", "LOC", "GPE", "PRODUCT", "EVENT", "WORK_OF_ART", "ORG")
    val body =
      s"{\"articles\": [{\"text\": ${Json.toJson(text)}}],\"model\": \"$model\",\"entities\": ${Json
          .toJson(entityTypes)}}"

    val before = System.currentTimeMillis

    wsClient
      .url(url)
      .withHttpHeaders(
        ("API-KEY", key),
        ("accept", "application/json"),
        ("Content-Type", "application/json")
      )
      .post(body)
      .map { response =>
        val after = System.currentTimeMillis
        response.status match {
          case 200 => {
            logger.info(s"Request to ${config.nerApiUrl} succeeded in ${after - before}ms")

            val entitiesJson = response.json.result("result")(0)("ents")
            entitiesJson.validate[List[NERResult]] match {
              case JsSuccess(value, _) =>
                Right(
                  value.map(entity =>
                    EntityInText(
                      word = entity.text,
                      range = TextRange(
                        from = entity.start + offset,
                        to = entity.end + offset
                      )
                    )
                  )
                )
              case JsError(error) =>
                Left(new Error(error.toString()))
            }
          }
          case _ => {
            logger.info(s"Request to ${config.nerApiUrl} failed in ${after - before}ms")

            Left(new Error(s"${response.status} ${response.statusText}"))
          }
        }
      }
  }
}
