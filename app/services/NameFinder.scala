package services

import opennlp.tools.namefind.{NameFinderME, TokenNameFinderModel}
import opennlp.tools.sentdetect.{SentenceDetectorME, SentenceModel}
import opennlp.tools.tokenize.{TokenizerME, TokenizerModel}
import opennlp.tools.util.Span

case class NameResult(from: Int, to: Int, text: String)

/**
  * A service to extract proper names from document using Named Entity Recognition.
  */
class NameFinder(
    nameFinderModel: TokenNameFinderModel,
    tokenizerModel: TokenizerModel,
    sentenceModel: SentenceModel
) {
  val sentenceDetector = new SentenceDetectorME(sentenceModel)
  val tokenizer = new TokenizerME(tokenizerModel)
  val nameFinder = new NameFinderME(nameFinderModel)

  def findNames(document: String): List[NameResult] = {
    val documentSentenceSpans = sentenceDetector.sentPosDetect(document)
    // We create spans and tokens here, to allow us to map back
    // to the original document if entities are found.
    val sentencesAndSentenceTokens = documentSentenceSpans.map(
      sentenceSpan =>
        (
          document
            .slice(sentenceSpan.getStart, sentenceSpan.getEnd),
          sentenceSpan,
          tokenizer.tokenizePos(
            document.slice(sentenceSpan.getStart, sentenceSpan.getEnd)
          )
        )
    )
    sentencesAndSentenceTokens.flatMap {
      case (sentence, sentenceSpan, sentenceTokenSpans) => {
        val sentenceTokenStrings =
          sentenceTokenSpans.map(_.getCoveredText(sentence).toString)
        val nameSpans = nameFinder.find(sentenceTokenStrings)

        val namePositions = nameSpans.map {
          extractPositionFromSpan(
            _,
            sentenceTokenSpans
          )
        }

        namePositions.flatten.map {
          case (start, end) =>
            NameResult(
              start + sentenceSpan.getStart,
              end + sentenceSpan.getStart,
              sentence.slice(start, end)
            )
        }
      }
    }.toList
  }

  /**
    * Given a span representing a range of tokens in a sentence, returns the string
    * corresponding to those tokens and the range that string covers in the sentence.
    *
    * @param tokenSpan The span of the token(s) in the sentence.
    * @param sentence The sentence text.
    * @param sentenceSpans The tokenized sentence.
    */
  private def extractPositionFromSpan(
      tokenSpan: Span,
      sentenceSpans: Array[Span]
  ): Option[(Int, Int)] = {
    val spans = sentenceSpans.slice(tokenSpan.getStart, tokenSpan.getEnd)
    for {
      startSpan <- spans.headOption
      endSpan <- spans.lastOption
    } yield {
      val start = startSpan.getStart
      val end = endSpan.getEnd
      (
        start,
        end
      )
    }
  }
}
