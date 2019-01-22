package controllers

import model.{CheckQuery, RuleMatch}
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc._
import services.LanguageToolInstancePool

import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
class ApiController(cc: ControllerComponents, lt: LanguageToolInstancePool)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok("{}")
  }

  def healthcheck() = Action { implicit request: Request[AnyContent] =>
    Ok("""{ "healthy" : "true" }""")
  }

  def check = Action(parse.json).async { request =>
    val result = for {
      checkQuery <- request.body.validate[CheckQuery]
    } yield {
      val futureResults = lt.check(checkQuery)

      futureResults.map { results =>
        Ok(Json.obj(
          "input" -> checkQuery.text,
          "results" -> Json.toJson(results)
        ))
      }.recover {
        case e: Exception => Results.InternalServerError(e.getMessage)
        case _ => InternalServerError("There was an unknown problem validating this text")
      }
    }

    result.fold(
      error => Future.successful(BadRequest(s"Invalid request: $error")),
      identity
    )
  }


  def refresh = Action { implicit request: Request[AnyContent] =>
    // @todo
    Ok
  }
}
