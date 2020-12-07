package services

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffMatcher._
import model.TextRange

class SentenceHelperTest extends AsyncFlatSpec with Matchers {

  behavior of "getFirstWordsInSentences"

  it should "return sentence starts, including the word and range covered" in {
    val sentenceTokenizer = new SentenceHelpers()
    val firstWordsInSentences = sentenceTokenizer.getFirstWordsInSentences(
      "Allowed to have up to 15 people in their home per day, and this rule applies to holiday accomodation. Cafes, bars, and restaurants will be able to seat 100 indoors and 200 outdoors, within the density limit"
    )
    firstWordsInSentences should matchTo(
      List(
        WordInSentence(
          "Allowed to have up to 15 people in their home per day, and this rule applies to holiday accomodation.",
          "Allowed",
          TextRange(0, 7)
        ),
        WordInSentence(
          "Cafes, bars, and restaurants will be able to seat 100 indoors and 200 outdoors, within the density limit",
          "Cafes",
          TextRange(102, 107)
        )
      )
    )
  }
}
