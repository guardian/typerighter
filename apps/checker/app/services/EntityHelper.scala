package services

import play.api.libs.json.{JsError, JsSuccess, Json, Reads}

import play.api.libs.ws.WSClient
import utils.CheckerConfig

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class EntityHelper(wsClient: WSClient, config: CheckerConfig) {
  case class NERResult(label: String, text: String, start: Int, end: Int)

  object NERResult {
    implicit val reads: Reads[NERResult] = Json.reads[NERResult]
  }

  def getEntityResultFromNERService(text: String): Future[Either[Error, List[String]]] = {
    val url = config.nerApiUrl
    val key = config.nerApiKey
    val model = "en_core_web_trf"
    val entityTypes =
      List("PERSON", "NORP", "FAC", "LOC", "GPE", "PRODUCT", "EVENT", "WORK_OF_ART", "ORG")
    val body =
      s"{\"articles\": [{\"text\": ${Json.toJson(text)}}],\"model\": \"$model\",\"entities\": ${Json
          .toJson(entityTypes)}}"

    wsClient
      .url(url)
      .withHttpHeaders(
        ("API-KEY", key),
        ("accept", "application/json"),
        ("Content-Type", "application/json")
      )
      .post(body)
      .map { response =>
        response.status match {
          case 200 => {
            val entitiesJson = response.json.result("result")(0)("ents")
            entitiesJson.validate[List[NERResult]] match {
              case JsSuccess(value, _) =>
                Right(value.map(_.text))
              case JsError(error) =>
                Left(new Error(error.toString()))
            }
          }
          case _ => Left(new Error(s"${response.status} ${response.statusText}"))
        }
      }
  }
}
