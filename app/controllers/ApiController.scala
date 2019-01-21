package controllers

import model.{CheckQuery, RuleMatch}
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc._
import services.LanguageToolCategoryHandler

import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
class ApiController(cc: ControllerComponents, lt: LanguageToolCategoryHandler)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok("{}")
  }

  def healthcheck() = Action { implicit request: Request[AnyContent] =>
    Ok("""{ "healthy" : "true" }""")
  }

  def check = Action(parse.json).async { request =>
    val maybeQuery = request.body.validate[CheckQuery] match {
      case query: CheckQuery => Some(query)
      case _ => None
    }
    val futureResult = maybeQuery.map { checkQuery =>
      lt.check(checkQuery).map { result =>
        Ok(Json.obj(
          "input" -> checkQuery.text,
          "result" -> result
        ))
      }
    }
    futureResult match {
      case Some(f) => f
      case _ => Future.successful(BadRequest("Something went wrong"))
    }
  }


  def refresh = Action { implicit request: Request[AnyContent] =>
    // @todo
    Ok
  }
}
