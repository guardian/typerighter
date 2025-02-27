package user_feedback

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.{Failure, Success, Try}

// Event case class that matches the JSON structure
case class AppEvent(
                     app: String,
                     stage: String,
                     documentUrl: String,
                     feedbackMessage: String,
                     matcherType: String,
                     ruleId: String,
                     suggestion: Option[String],
                     matchId: String,
                     matchIsMarkedAsCorrect: Boolean,
                     matchIsAdvisory: Boolean,
                     matchHasReplacement: Boolean,
                     matchedText: String,
                     matchContext: String
                   )

object AppEvent {
  // JSON format definition using play-json combinators
  implicit val appEventReads: Reads[AppEvent] = (
    (JsPath \ "app").read[String] and
      (JsPath \ "stage").read[String] and
      (JsPath \ "documentUrl").read[String] and
      (JsPath \ "feedbackMessage").read[String] and
      (JsPath \ "matcherType").read[String] and
      (JsPath \ "ruleId").read[String] and
      (JsPath \ "suggestion").readNullable[String] and
      (JsPath \ "matchId").read[String] and
      (JsPath \ "matchIsMarkedAsCorrect").read[Boolean] and
      (JsPath \ "matchIsAdvisory").read[Boolean] and
      (JsPath \ "matchHasReplacement").read[Boolean] and
      (JsPath \ "matchedText").read[String] and
      (JsPath \ "matchContext").read[String]
    )(AppEvent.apply _)

  // Optional: Add writes if you need to serialize AppEvent back to JSON
  implicit val appEventWrites: Writes[AppEvent] = Json.writes[AppEvent]
}

class EventProcessor extends RequestHandler[APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent] {
  override def handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    val logger = context.getLogger
    logger.log(s"Received event: ${input.getBody}")

    val response = new APIGatewayProxyResponseEvent()

    // Parse the incoming JSON body
    Try(Json.parse(input.getBody)) match {
      case Success(jsonBody) =>
        // Try to convert the JSON to an AppEvent
        jsonBody.validate[AppEvent] match {
          case JsSuccess(appEvent, _) =>
            // Successfully parsed the event, process it
            processEvent(appEvent, logger)

            // Return a successful response
            response.setStatusCode(200)
            response.setBody(Json.obj("status" -> "success", "message" -> "Event processed successfully").toString())

          case JsError(errors) =>
            // JSON validation failed
            logger.log(s"Failed to parse JSON: $errors")
            response.setStatusCode(400)
            response.setBody(Json.obj("status" -> "error", "message" -> "Invalid event format").toString())
        }

      case Failure(exception) =>
        // Failed to parse the JSON
        logger.log(s"Failed to parse request body as JSON: ${exception.getMessage}")
        response.setStatusCode(400)
        response.setBody(Json.obj("status" -> "error", "message" -> "Invalid JSON").toString())
    }

    response
  }

  private def processEvent(event: AppEvent, logger: com.amazonaws.services.lambda.runtime.LambdaLogger): Unit = {
    // TODO: Implement your business logic here
    logger.log(s"Processing event for app: ${event.app}, matchId: ${event.matchId}")

    // Example processing:
    if (event.matchIsMarkedAsCorrect) {
      logger.log(s"Match ${event.matchId} marked as correct for rule ${event.ruleId}")
      // Maybe store this feedback in a database, or forward to another service
    } else {
      logger.log(s"Processing incorrect match: ${event.matchedText} with context: ${event.matchContext}")
      // Apply logic for incorrect matches
    }

    // Handle suggestions if present
    event.suggestion.foreach { suggestionText =>
      logger.log(s"Suggestion provided: $suggestionText")
      // Process the suggestion
    }
  }
}