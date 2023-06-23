package services

import com.gu.typerighter.model.TextRange
import opennlp.tools.sentdetect.{SentenceDetectorME, SentenceModel}
import opennlp.tools.tokenize.SimpleTokenizer
import opennlp.tools.util.Span

case class WordInSentence(sentence: String, word: String, range: TextRange)

/** A service to extract proper names from documents.
  */
class SentenceHelpers() {
  val sentenceInputFile = getClass.getClassLoader.getResourceAsStream(
    "resources/opennlp/opennlp-en-ud-ewt-sentence-1.0-1.9.3.bin"
  )
  val sentenceModel = new SentenceModel(sentenceInputFile)
  val sentenceDetector = new SentenceDetectorME(sentenceModel)
  val tokenizer = SimpleTokenizer.INSTANCE

  def getFirstWordsInSentences(text: String): List[WordInSentence] = {
    val sentences = sentenceDetector.sentPosDetect(text).toList

    for {
      sentence <- sentences
      wordInSentence <- maybeGetFirstWordFromSentence(
        sentence.getCoveredText(text).toString,
        sentence.getStart
      )
    } yield wordInSentence
  }

  private def maybeGetFirstWordFromSentence(sentence: String, startPos: Int): Option[WordInSentence] = {
    val tokens = tokenizer.tokenizePos(sentence).toList

    tokens
      .map(token => (token, token.getCoveredText(sentence).toString))
      .find {
        // Only consider tokens that contain letter characters as words
        case (_, tokenStr) => tokenStr.exists(_.isLetter)
      }
      .map { case (token, tokenStr) =>
        val indexOfLastNonWordToken = tokenStr.lastIndexWhere(!_.isLetter)

        // OpenNLP sometimes includes non-word chars at the beginning of tokens
        // that contain words. If our word begins with a non-word token, remove it
        // from the span and adjust the range.
        val (wordToken, wordTokenStr) =
          if (indexOfLastNonWordToken == -1 || tokenStr.headOption.forall(_.isLetter))
            (token, tokenStr)
          else {
            val (_, wordStr) = tokenStr.splitAt(indexOfLastNonWordToken + 1)
            val from = indexOfLastNonWordToken + 1
            val to = from + wordStr.length

            (new Span(from, to), wordStr)
          }

        WordInSentence(
          sentence,
          wordTokenStr,
          TextRange(startPos + wordToken.getStart, startPos + wordToken.getEnd)
        )
      }
  }
}
