import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger}
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.gu.pandomainauth.model.{Authenticated, AuthenticatedUser, AuthenticationStatus, User}
import models.UserFeedback
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
  val exampleUser = User(
    firstName = "a",
    lastName = "user",
    email = "a.user@guardian.co.uk",
    avatarUrl = None
  )
  val authenticatedResponse = Authenticated(
    AuthenticatedUser(
      user = exampleUser,
      authenticatingSystem = "authenticating-system",
      Set(),
      expires = 1L,
      multiFactor = true
    )
  )

  "EventProcessor" should "successfully process a valid event" in {
    val validFeedback = UserFeedback(
      app = "app",
      stage = "TEST",
      documentUrl = "example.url",
      feedbackMessage = "A feedback message"
    )

    val response = getLambdaResponse(body = Json.toJson(validFeedback).toString)
    response.getStatusCode shouldBe 200

    val responseBody = Json.parse(response.getBody)
    (responseBody \ "status").as[String] shouldBe "success"
  }

  private def getLambdaResponse(
      body: String,
      authenticationStatus: AuthenticationStatus = authenticatedResponse
  ) = {
    val snsEventSender = mock[SNSEventSender]
    val config = mock[UserFeedbackConfig]

    val lambdaAuth = mock[LambdaAuth]
    when(lambdaAuth.authenticateCookie(any, any, any)).thenReturn(authenticationStatus)

    val context = mock[Context]
    val processor = new Handler(config, lambdaAuth, snsEventSender)

    val logger = mock[LambdaLogger]
    when(context.getLogger).thenReturn(logger)

    val request = new APIGatewayProxyRequestEvent()
    request.setBody(body)
    request.setHeaders(Map("Cookie" -> "auth").asJava)

    processor.handleRequest(request, context)
  }

  it should "return a 400 response for invalid JSON" in {
    val invalidJson = "{invalid: json"

    val response = getLambdaResponse(invalidJson)

    response.getStatusCode shouldBe 400
    val responseBody = Json.parse(response.getBody)

    (responseBody \ "status").as[String] shouldBe "error"
    (responseBody \ "message").as[String] should include("Error parsing JSON")
  }

  it should "return a 400 response when required fields are missing" in {
    val incompleteJson =
      """{
        |  "app": "grammar-check",
        |  "stage": "production"
        |}""".stripMargin

    val response = getLambdaResponse(incompleteJson)

    response.getStatusCode shouldBe 400
    val responseBody = Json.parse(response.getBody)

    (responseBody \ "status").as[String] shouldBe "error"
    (responseBody \ "message").as[String] should include("error.path.missing")
  }
}
