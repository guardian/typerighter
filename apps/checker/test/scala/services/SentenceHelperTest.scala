package services

import com.gu.typerighter.model.TextRange
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffShouldMatcher._
import com.softwaremill.diffx.generic.auto._

class SentenceHelperTest extends AsyncFlatSpec with Matchers {

  val sentenceTokenizer = new SentenceHelpers()
  val charactersThatProduceNonWordTokens = List(
    "\"",
    "'",
    "`",
    "{",
    "[",
    "(",
    "“",
    "‘",
    "-",
    "–",
    "—"
  )

  behavior of "getFirstWordsInSentences"

  it should "return sentence starts, including the word and range covered" in {
    val firstWordsInSentences = sentenceTokenizer.getFirstWordsInSentences(
      "Allowed to have up to 15 people in their home per day, and this rule applies to holiday accomodation. Cafes, bars, and restaurants will be able to seat 100 indoors and 200 outdoors, within the density limit"
    )
    firstWordsInSentences shouldMatchTo (List(
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
    ))
  }

  charactersThatProduceNonWordTokens.foreach { nonWordChar =>
    it should s"ignore non-word tokens when finding sentence starts: $nonWordChar" in {
      val firstWordsInSentences = sentenceTokenizer.getFirstWordsInSentences(
        s"Allowed to have up to 15 people in their home per day, and this rule applies to holiday accomodation. ${nonWordChar}Cafes, bars, and restaurants will be able to seat 100 indoors and 200 outdoors, within the density limit"
      )
      firstWordsInSentences shouldMatchTo (List(
        WordInSentence(
          "Allowed to have up to 15 people in their home per day, and this rule applies to holiday accomodation.",
          "Allowed",
          TextRange(0, 7)
        ),
        WordInSentence(
          s"${nonWordChar}Cafes, bars, and restaurants will be able to seat 100 indoors and 200 outdoors, within the density limit",
          "Cafes",
          TextRange(103, 108)
        )
      ))
    }

    it should s"not ignore words that contain non-word tokens when finding sentence starts: $nonWordChar" in {
      val firstWordsInSentences = sentenceTokenizer.getFirstWordsInSentences(
        s"Allowed to have up to 15 people in their home per day, and this rule applies to holiday accomodation. Other${nonWordChar} cafes, bars, and restaurants will be able to seat 100 indoors and 200 outdoors, within the density limit"
      )
      firstWordsInSentences shouldMatchTo (List(
        WordInSentence(
          "Allowed to have up to 15 people in their home per day, and this rule applies to holiday accomodation.",
          "Allowed",
          TextRange(0, 7)
        ),
        WordInSentence(
          s"Other${nonWordChar} cafes, bars, and restaurants will be able to seat 100 indoors and 200 outdoors, within the density limit",
          "Other",
          TextRange(102, 107)
        )
      ))
    }
  }

  charactersThatProduceNonWordTokens.foreach { nonWordChar =>
    it should s"ignore multiple non-word tokens when finding sentence starts: $nonWordChar" in {
      val firstWordsInSentences = sentenceTokenizer.getFirstWordsInSentences(
        s"${nonWordChar}${nonWordChar}Cafes, bars, and restaurants will be able to seat 100 indoors and 200 outdoors, within the density limit"
      )
      firstWordsInSentences shouldMatchTo (List(
        WordInSentence(
          s"${nonWordChar}${nonWordChar}Cafes, bars, and restaurants will be able to seat 100 indoors and 200 outdoors, within the density limit",
          "Cafes",
          TextRange(2, 7)
        )
      ))
    }
  }

  it should s"not give any results if the sentence consists entirely of non word tokens" in {
    val nonWordChar = charactersThatProduceNonWordTokens.head
    val firstWordsInSentences = sentenceTokenizer.getFirstWordsInSentences(
      s"$nonWordChar$nonWordChar$nonWordChar$nonWordChar$nonWordChar$nonWordChar"
    )
    firstWordsInSentences shouldMatchTo (List.empty[WordInSentence])
  }
}
