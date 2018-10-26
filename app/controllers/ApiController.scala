package controllers

import play.api.libs.json.Json
import play.api.mvc._
import services.LanguageTool

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
class ApiController (cc: ControllerComponents, lt: LanguageTool) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok("{}")
  }

  def check = Action(parse.formUrlEncoded) { request =>
    val result: Option[Result] = for {
      textSeq <- request.body.get("text")
      text <- textSeq.headOption
    } yield {
      val results = lt.check(text)
      val json = Json.obj(
        "input" -> text,
        "results" -> Json.toJson(results)
      )
      Ok(json)
    }
    result.getOrElse(BadRequest("Invalid request, fields missing"))
  }
}
