package controllers

import com.gu.contentapi.json.CirceEncoders._
import io.circe.syntax._
import play.api.mvc._
import com.gu.typerighter.controllers.AppAuthActions
import com.gu.typerighter.lib.CommonConfig
import services.ContentClient

import scala.concurrent.ExecutionContext

class CapiProxyController(
    val controllerComponents: ControllerComponents,
    contentClient: ContentClient,
    val config: CommonConfig
)(implicit ec: ExecutionContext)
    extends BaseController
    with AppAuthActions {

  def searchContent(
      query: String,
      tags: Option[List[String]],
      sections: Option[List[String]],
      page: Option[Int]
  ) = APIAuthAction.async {
    contentClient
      .searchContent(
        query,
        tags.getOrElse(Nil),
        sections.getOrElse(Nil),
        page.getOrElse(1)
      )
      .map { result => Ok(result.asJson.toString).as(JSON) }
  }

  def searchTags(queryStr: String) = APIAuthAction.async {
    contentClient.searchTags(queryStr).map { result => Ok(result.asJson.toString).as(JSON) }
  }

  def searchSections(queryStr: String) = APIAuthAction.async {
    contentClient.searchSections(queryStr).map { result => Ok(result.asJson.toString).as(JSON) }
  }
}
