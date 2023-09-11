package model

import play.api.libs.json.{Format, Json}

case class PaginatedResponse[Data](
    data: List[Data],
    pageSize: Int,
    page: Int,
    pages: Int,
    total: Int
)

object PaginatedResponse {
  implicit def format[Data: Format]: Format[PaginatedResponse[Data]] =
    Json.format[PaginatedResponse[Data]]
}
