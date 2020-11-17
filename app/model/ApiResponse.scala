package model

import play.api.libs.json._
import com.scalatsi.TSType
import com.scalatsi.TypescriptType

/**
  * A 'status' field should included with each API response.
  */
object ApiResponseStatus {
  val OK = "OK"
  val ERROR = "ERROR"
}

/**
  * The Typerighter API response when returning matches to a client.
  */
case class ApiResponse(blocks: List[TextBlock], categoryIds: List[String], matches: List[RuleMatch])

object ApiResponse {
  private val envelope = Map(
    "type" -> "VALIDATOR_RESPONSE",
    "status" -> ApiResponseStatus.OK
  )

  implicit val writes = new Writes[ApiResponse] {
    def writes(response: ApiResponse) = Json.obj(
      "categoryIds" -> response.categoryIds,
      "blocks" -> response.blocks,
      "matches" -> response.matches,
    ) ++ Json.toJsObject(envelope)
  }
}

/**
  * The Typerighter API response when all work is complete for the current document.
  *
  * Used for the streaming endpoint.
  */
case class ApiWorkComplete()

object ApiWorkComplete {
  private val envelope = Map(
    "type" -> "VALIDATOR_WORK_COMPLETE",
    "status" -> ApiResponseStatus.OK
  )

  implicit val writes = new Writes[ApiWorkComplete] {
    def writes(model: ApiWorkComplete) = Json.toJsObject(envelope)
  }
}

/**
  * The Typerighter API response when an error has occurred.
  */
case class ApiError(message: String, id: Option[String] = None)

object ApiError {
  private val envelope = Map(
    "type" -> "VALIDATOR_ERROR",
    "status" -> ApiResponseStatus.ERROR
  )

  implicit val writes = new Writes[ApiError] {
    def writes(response: ApiError) = Json.obj(
      "status" -> ApiResponseStatus.ERROR,
      "message" -> response.message
    ) ++ Json.toJsObject(envelope)
  }
}

