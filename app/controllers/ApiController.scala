package controllers

import play.api.libs.json.Json
import play.api.mvc._
import services.LanguageTool

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
class ApiController (cc: ControllerComponents, lt: LanguageTool) extends AbstractController(cc) {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok("{}")
  }

  def healthcheck() = Action { implicit request: Request[AnyContent] =>
    Ok("""{ "healthy" : "true" }""")
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
