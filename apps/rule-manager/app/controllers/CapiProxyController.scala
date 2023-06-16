package controllers

import com.gu.pandomainauth.PublicSettings
import com.gu.typerighter.lib.PandaAuthentication
import play.api.mvc._

import scala.concurrent.ExecutionContext

class CapiProxyController(
    cc: ControllerComponents,
    contentClient: ContentClient,
    val publicSettings: PublicSettings
)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with PandaAuthentication {

  def searchContent(
      query: String,
      tags: Option[List[String]],
      sections: Option[List[String]],
      page: Option[Int]
  ) = ApiAuthAction.async {
    contentClient
      .searchContent(
        query,
        tags.getOrElse(Nil),
        sections.getOrElse(Nil),
        page.getOrElse(1)
      )
      .map { result => Ok(result.asJson.toString).as(JSON) }
  }

  def searchTags(queryStr: String) = ApiAuthAction.async {
    contentClient.searchTags(queryStr).map { result => Ok(result.asJson.toString).as(JSON) }
  }

  def searchSections(queryStr: String) = ApiAuthAction.async {
    contentClient.searchSections(queryStr).map { result => Ok(result.asJson.toString).as(JSON) }
  }
}
