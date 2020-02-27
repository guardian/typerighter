package controllers

import com.gu.contentapi.json.CirceEncoders._
import io.circe.syntax._
import play.api.libs.json.JsValue
import play.api.mvc._
import services.{CapiSearchParams, ContentClient}

import scala.concurrent.{ExecutionContext, Future}

class CapiProxyController(cc: ControllerComponents, contentClient: ContentClient)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def searchContent: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CapiSearchParams].asEither match {
      case Right(query) => contentClient.searchContent(query).map { result => Ok(result.asJson.toString).as(JSON) }
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
