package controllers

import play.api.mvc._
import com.gu.pandomainauth.PublicSettings
import com.gu.typerighter.lib.PandaAuthentication

/**
 * The controller for the index pages.
 */
class HomeController(cc: ControllerComponents, val publicSettings: PublicSettings) extends AbstractController(cc) with PandaAuthentication {
  def index() = ApiAuthAction {
    Ok(views.html.index())
  }

  def healthcheck() = Action {
    Ok("""{ "healthy" : "true" }""")
  }
}
