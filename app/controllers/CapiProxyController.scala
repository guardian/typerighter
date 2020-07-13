package controllers

import com.gu.contentapi.json.CirceEncoders._
import io.circe.syntax._
import play.api.mvc._
import services.{ContentClient}

import scala.concurrent.{ExecutionContext, Future}

class CapiProxyController(cc: ControllerComponents, contentClient: ContentClient)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def searchContent(query: String, tags: Option[List[String]], sections: Option[List[String]], page: Option[Int]) = Action.async {
    contentClient.searchContent(
      query,
      tags.getOrElse(Nil),
      sections.getOrElse(Nil),
      page.getOrElse(1)
    ).map { result => Ok(result.asJson.toString).as(JSON) }
  }

  def searchTags(queryStr: String) = Action.async {
    contentClient.searchTags(queryStr).map { result => Ok(result.asJson.toString).as(JSON) }
  }

  def searchSections(queryStr: String) = Action.async {
    contentClient.searchSections(queryStr).map { result => Ok(result.asJson.toString).as(JSON) }
  }
}
