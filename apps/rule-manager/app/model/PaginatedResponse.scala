package model

import play.api.libs.json.{Format, Json}

import scala.annotation.nowarn

case class PaginatedResponse[Data](
    data: List[Data],
    pageSize: Int,
    page: Int,
    pages: Int,
    total: Int
)

object PaginatedResponse {
  @nowarn
  implicit def format[Data](implicit _underlying: Format[Data]): Format[PaginatedResponse[Data]] =
    Json.format[PaginatedResponse[Data]]
}
