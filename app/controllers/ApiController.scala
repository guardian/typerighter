package controllers

import model.CheckQuery
import play.api.libs.json.{Json, JsResult, JsValue}
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

  def check: Action[JsValue] = Action(parse.json) { request =>
    val result: JsResult[Result] = for {
      checkQuery <- request.body.validate[CheckQuery]
    } yield {
      val results = lt.check(checkQuery.text)
      val json = Json.obj(
        "input" -> checkQuery.text,
        "results" -> Json.toJson(results)
      )
      Ok(json)
    }
    result.fold(
      error => BadRequest(s"Invalid request: $error"),
      identity
    )
  }

  def refresh = Action { implicit request: Request[AnyContent] =>
    lt.reingestRules()
    Ok
  }
}
