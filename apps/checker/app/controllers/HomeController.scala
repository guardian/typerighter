package controllers

import play.api.mvc._
import com.gu.pandomainauth.PublicSettings
import com.gu.typerighter.lib.PandaAuthentication

import scala.concurrent.ExecutionContext

/** The controller for the index pages.
  */
class HomeController(cc: ControllerComponents, val publicSettings: PublicSettings)(implicit
    ec: ExecutionContext
) extends AbstractController(cc)
    with PandaAuthentication {
  def index() = ApiAuthAction { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def healthcheck() = Action { implicit request: Request[AnyContent] =>
    Ok("""{ "healthy" : "true" }""")
  }
}
