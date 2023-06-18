package controllers

import com.gu.contentapi.json.CirceEncoders._
import io.circe.syntax._
import play.api.mvc._
<<<<<<< HEAD
import com.gu.typerighter.controllers.PandaAuthController
=======
import com.gu.typerighter.controllers.AppAuthActions
>>>>>>> 7130b55a (Refactor configuration to pass pan-domain-config as a part of CommonConfig, and adjust controller configuration to suit HMACAuthActions)
import com.gu.typerighter.lib.CommonConfig
import services.ContentClient

import scala.concurrent.ExecutionContext

class CapiProxyController(
<<<<<<< HEAD
    controllerComponents: ControllerComponents,
    contentClient: ContentClient,
    config: CommonConfig
)(implicit ec: ExecutionContext)
    extends PandaAuthController(controllerComponents, config) {
=======
    val controllerComponents: ControllerComponents,
    contentClient: ContentClient,
    val config: CommonConfig
)(implicit ec: ExecutionContext)
    extends BaseController
    with AppAuthActions {
>>>>>>> 7130b55a (Refactor configuration to pass pan-domain-config as a part of CommonConfig, and adjust controller configuration to suit HMACAuthActions)

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
