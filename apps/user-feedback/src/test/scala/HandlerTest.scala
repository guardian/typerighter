import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger}
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.gu.pandomainauth.model.{Authenticated, AuthenticatedUser, User}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import services.{LambdaAuth, SNSEventSender}
import utils.UserFeedbackConfig

import scala.jdk.CollectionConverters._

class HandlerTest extends AnyFlatSpec with Matchers with MockitoSugar {
  "EventProcessor" should "successfully process a valid event" in {
    // Arrange

    val snsEventSender = mock[SNSEventSender]
    val config = mock[UserFeedbackConfig]
    val lambdaAuth = mock[LambdaAuth]
    when(lambdaAuth.authenticateCookie(any, any, any)).thenReturn(Authenticated(AuthenticatedUser(
      User("a", "user", "a.user@guardian.co.uk", None), "authenticating-system", Set(), 1L, true)))
    val processor = new Handler(config, lambdaAuth, snsEventSender)
    val context = mock[Context]
    val logger = mock[LambdaLogger]
    when(context.getLogger).thenReturn(logger)

    val validJson =
      """{
        |  "app": "grammar-check",
        |  "stage": "production",
        |  "documentUrl": "https://example.com/document/123",
        |  "feedbackMessage": "User feedback for this match",
        |  "matcherType": "grammar",
        |  "ruleId": "MISSING_COMMA",
        |  "suggestion": "Add a comma here",
        |  "matchId": "abc-123-456",
        |  "matchIsMarkedAsCorrect": true,
        |  "matchIsAdvisory": false,
        |  "matchHasReplacement": true,
        |  "matchedText": "however they",
        |  "matchContext": "The team worked hard however they missed the deadline."
        |}""".stripMargin

    val request = new APIGatewayProxyRequestEvent()
    request.setBody(validJson)
    request.setHeaders(Map("Cookie" -> "auth").asJava)

    // Act
    val response = processor.handleRequest(request, context)

    // Assert
    response.getStatusCode shouldBe 200
    val responseBody = Json.parse(response.getBody)
    (responseBody \ "status").as[String] shouldBe "success"
  }

  it should "handle a valid event with null suggestion" in {
    // Arrange
    val processor = new Handler()
    val context = mock[Context]

    val validJsonWithNullSuggestion =
      """{
        |  "app": "grammar-check",
        |  "stage": "production",
        |  "documentUrl": "https://example.com/document/123",
        |  "feedbackMessage": "User feedback for this match",
        |  "matcherType": "grammar",
        |  "ruleId": "PASSIVE_VOICE",
        |  "suggestion": null,
        |  "matchId": "abc-123-456",
        |  "matchIsMarkedAsCorrect": false,
        |  "matchIsAdvisory": true,
        |  "matchHasReplacement": false,
        |  "matchedText": "was written by",
        |  "matchContext": "The letter was written by John."
        |}""".stripMargin

    val request = new APIGatewayProxyRequestEvent()
    request.setBody(validJsonWithNullSuggestion)

    // Act
    val response = processor.handleRequest(request, context)

    // Assert
    response.getStatusCode shouldBe 200
    val responseBody = Json.parse(response.getBody)
    (responseBody \ "status").as[String] shouldBe "success"
  }

  it should "return a 400 response for invalid JSON" in {
    // Arrange
    val processor = new Handler()
    val context = mock[Context]

    val invalidJson = "{invalid: json"

    val request = new APIGatewayProxyRequestEvent()
    request.setBody(invalidJson)

    // Act
    val response = processor.handleRequest(request, context)

    // Assert
    response.getStatusCode shouldBe 400
    val responseBody = Json.parse(response.getBody)
    (responseBody \ "status").as[String] shouldBe "error"
    (responseBody \ "message").as[String] shouldBe "Invalid JSON"
  }

  it should "return a 400 response when required fields are missing" in {
    // Arrange
    val processor = new Handler()
    val context = mock[Context]

    val incompleteJson =
      """{
        |  "app": "grammar-check",
        |  "stage": "production"
        |}""".stripMargin

    val request = new APIGatewayProxyRequestEvent()
    request.setBody(incompleteJson)

    // Act
    val response = processor.handleRequest(request, context)

    // Assert
    response.getStatusCode shouldBe 400
    val responseBody = Json.parse(response.getBody)
    (responseBody \ "status").as[String] shouldBe "error"
    (responseBody \ "message").as[String] shouldBe "Invalid event format"
  }
}