package controllers

import com.gu.contentapi.json.CirceEncoders._
import io.circe.syntax._
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.libs.json.{Json, Reads, Writes}
import services.{ContentClient}

import scala.concurrent.{ExecutionContext, Future}

object CapiSearchParams {
  implicit val writes: Writes[CapiSearchParams] = Json.writes[CapiSearchParams]
  implicit val reads: Reads[CapiSearchParams] = Json.reads[CapiSearchParams]
}

case class CapiSearchParams(query: String, tags: Option[List[String]] = None, sections: Option[List[String]] = None)

class CapiProxyController(cc: ControllerComponents, contentClient: ContentClient)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def searchContent: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CapiSearchParams].asEither match {
      case Right(query) => contentClient.searchContent(
          query.query,
          query.tags.getOrElse(Nil),
          query.sections.getOrElse(Nil))
        .map { result => Ok(result.asJson.toString).as(JSON) }
      case Left(error) => Future.successful(BadRequest(s"Invalid request: $error"))
    }
  }

  def searchTags(queryStr: String) = Action.async {
    contentClient.searchTags(queryStr).map { result => Ok(result.asJson.toString).as(JSON) }
  }

  def searchSections(queryStr: String) = Action.async {
    contentClient.searchSections(queryStr).map { result => Ok(result.asJson.toString).as(JSON) }
  }
}
