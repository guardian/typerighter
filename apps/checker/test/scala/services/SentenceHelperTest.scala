package services

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffMatcher._
import model.TextRange

class SentenceHelperTest extends AsyncFlatSpec with Matchers {

  val sentenceTokenizer = new SentenceHelpers()
  val charactersThatProduceNonWordTokens = List(
    ("\"", "\""),
    ("'", "'"),
    ("`", "`"),
    ("{", "}"),
    ("[", "]"),
    ("(", ")"),
    ("“", "”"),
    ("‘", "’)"),
    ("-", ""),
    ("–", ""),
    ("—", "")
  )

  behavior of "getFirstWordsInSentences"

  it should "return sentence starts, including the word and range covered" in {

    val firstWordsInSentences = sentenceTokenizer.getFirstWordsInSentences("Allowed to have up to 15 people in their home per day, and this rule applies to holiday accomodation. Cafes, bars, and restaurants will be able to seat 100 indoors and 200 outdoors, within the density limit")
    firstWordsInSentences should matchTo(List(
      WordInSentence("Allowed to have up to 15 people in their home per day, and this rule applies to holiday accomodation.", "Allowed", TextRange(0, 7)),
      WordInSentence("Cafes, bars, and restaurants will be able to seat 100 indoors and 200 outdoors, within the density limit", "Cafes", TextRange(102, 107)),
    ))
  }

  charactersThatProduceNonWordTokens.foreach {
    case (leftNonWordChar, rightNonWordChar) =>
      it should s"ignore non-word tokens when finding sentence starts: $leftNonWordChar $rightNonWordChar" in {
        val firstWordsInSentences = sentenceTokenizer.getFirstWordsInSentences(s"Allowed to have up to 15 people in their home per day, and this rule applies to holiday accomodation. ${leftNonWordChar}Cafes${rightNonWordChar}, bars, and restaurants will be able to seat 100 indoors and 200 outdoors, within the density limit")
        firstWordsInSentences should matchTo(List(
          WordInSentence("Allowed to have up to 15 people in their home per day, and this rule applies to holiday accomodation.", "Allowed", TextRange(0, 7)),
          WordInSentence(s"${leftNonWordChar}Cafes${rightNonWordChar}, bars, and restaurants will be able to seat 100 indoors and 200 outdoors, within the density limit", "Cafes", TextRange(103, 108)),
        ))
      }
  }

  charactersThatProduceNonWordTokens.foreach {
    case (leftNonWordChar, rightNonWordChar) =>
      it should s"ignore multiple non-word tokens when finding sentence starts: $leftNonWordChar $rightNonWordChar" in {
        val firstWordsInSentences = sentenceTokenizer.getFirstWordsInSentences(s"${leftNonWordChar}${leftNonWordChar}Cafes${rightNonWordChar}${rightNonWordChar}, bars, and restaurants will be able to seat 100 indoors and 200 outdoors, within the density limit")
        firstWordsInSentences should matchTo(List(
          WordInSentence(s"${leftNonWordChar}${leftNonWordChar}Cafes${rightNonWordChar}${rightNonWordChar}, bars, and restaurants will be able to seat 100 indoors and 200 outdoors, within the density limit", "Cafes", TextRange(2, 7)),
        ))
      }
  }

  it should s"not give any results if the sentence consists entirely of non word tokens" in {
    val nonWordChar = charactersThatProduceNonWordTokens.head._1
    val firstWordsInSentences = sentenceTokenizer.getFirstWordsInSentences(s"$nonWordChar$nonWordChar$nonWordChar$nonWordChar$nonWordChar$nonWordChar")
    firstWordsInSentences should matchTo(List.empty[WordInSentence])
  }
}
