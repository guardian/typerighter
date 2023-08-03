package fixtures

import com.gu.contentapi.client.model.v1.SearchResponse
import com.gu.contentapi.json.CirceDecoders._
import io.circe.parser.decode

import scala.io.Source

object CAPIFixtures {
  val searchResponseWithBodyField = {
    val source = Source.fromURL(getClass.getResource("/CAPI/searchResponseWithBodyField.json"))
    val input = decode[SearchResponse](source.mkString)
    source.close()
    input.toOption.get
  }
}
