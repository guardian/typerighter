import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger}
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsNull, JsValue, Json}

class EventProcessorSpec extends AnyFlatSpec with Matchers with MockitoSugar {
  "EventProcessor" should "successfully process a valid event" in {
    // Arrange
    val processor = new EventProcessor()
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

    // Act
    val response = processor.handleRequest(request, context)

    // Assert
    response.getStatusCode shouldBe 200
    val responseBody = Json.parse(response.getBody)
    (responseBody \ "status").as[String] shouldBe "success"
    verify(logger, atLeastOnce).log(anyString())
  }

  it should "handle a valid event with null suggestion" in {
    // Arrange
    val processor = new EventProcessor()
    val context = mock[Context]
    val logger = mock[LambdaLogger]

    when(context.getLogger).thenReturn(logger)

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
    val processor = new EventProcessor()
    val context = mock[Context]
    val logger = mock[LambdaLogger]

    when(context.getLogger).thenReturn(logger)

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
    val processor = new EventProcessor()
    val context = mock[Context]
    val logger = mock[LambdaLogger]

    when(context.getLogger).thenReturn(logger)

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

  "AppEvent JSON format" should "correctly parse valid JSON" in {
    // Arrange
    val validJson = Json.parse(
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
    )

    // Act
    val result = validJson.validate[AppEvent]

    // Assert
    result.isSuccess shouldBe true
    val event = result.get
    event.app shouldBe "grammar-check"
    event.stage shouldBe "production"
    event.documentUrl shouldBe "https://example.com/document/123"
    event.feedbackMessage shouldBe "User feedback for this match"
    event.matcherType shouldBe "grammar"
    event.ruleId shouldBe "MISSING_COMMA"
    event.suggestion shouldBe Some("Add a comma here")
    event.matchId shouldBe "abc-123-456"
    event.matchIsMarkedAsCorrect shouldBe true
    event.matchIsAdvisory shouldBe false
    event.matchHasReplacement shouldBe true
    event.matchedText shouldBe "however they"
    event.matchContext shouldBe "The team worked hard however they missed the deadline."
  }

  it should "handle null suggestion correctly" in {
    // Arrange
    val jsonWithNullSuggestion = Json.parse(
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
    )

    // Act
    val result = jsonWithNullSuggestion.validate[AppEvent]

    // Assert
    result.isSuccess shouldBe true
    val event = result.get
    event.suggestion shouldBe None
  }

  it should "also handle missing suggestion field" in {
    // Arrange
    val jsonWithMissingSuggestion = Json.parse(
      """{
        |  "app": "grammar-check",
        |  "stage": "production",
        |  "documentUrl": "https://example.com/document/123",
        |  "feedbackMessage": "User feedback for this match",
        |  "matcherType": "grammar",
        |  "ruleId": "PASSIVE_VOICE",
        |  "matchId": "abc-123-456",
        |  "matchIsMarkedAsCorrect": false,
        |  "matchIsAdvisory": true,
        |  "matchHasReplacement": false,
        |  "matchedText": "was written by",
        |  "matchContext": "The letter was written by John."
        |}""".stripMargin
    )

    // Act
    val result = jsonWithMissingSuggestion.validate[AppEvent]

    // Assert
    result.isSuccess shouldBe false
  }
}