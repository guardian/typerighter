package services

import opennlp.tools.namefind.{NameFinderME, TokenNameFinderModel}
import opennlp.tools.sentdetect.{SentenceDetectorME, SentenceModel}
import opennlp.tools.tokenize.{TokenizerME, TokenizerModel}
import opennlp.tools.util.Span

case class NameResult(from: Int, to: Int, text: String)

/**
  * A service to extract proper names from text using Named Entity Recognition.
  */
class NameFinder(
                  nameFinderModel: TokenNameFinderModel,
                  tokenizerModel: TokenizerModel,
                  sentenceModel: SentenceModel) {
  val sentenceDetector = new SentenceDetectorME(sentenceModel)
  val tokenizer = new TokenizerME(tokenizerModel)
  val nameFinder = new NameFinderME(nameFinderModel)

  def findNames(text: String): List[NameResult] = {
    val sentences = sentenceDetector.sentDetect(text)
    // We create spans and tokens here, to allow us to map back
    // to the original text if entities are found.
    val sentencesAsTokens = sentences.map(sentence =>
      (sentence, tokenizer.tokenizePos(sentence), tokenizer.tokenize(sentence)))
    sentencesAsTokens.flatMap({
      case (sentence, sentenceSpans, sentenceTokens) => {
        val spans = nameFinder.find(sentenceTokens)
        spans.map(extractPositionAndNameFromSpan(_, sentence, sentenceSpans))
      }
    }).toList.flatten
  }

  /**
    * Given a span representing a range of tokens in a sentence, returns the string
    * corresponding to those tokens and the range that string covers in the sentence.
    *
    * @param tokenSpan The span of the token(s) in the sentence.
    * @param sentence The sentence text.
    * @param sentenceSpans The tokenized sentence.
    */
  private def extractPositionAndNameFromSpan(tokenSpan: Span, sentence: String, sentenceSpans: Array[Span]): Option[NameResult] = {
    val spans = sentenceSpans.slice(tokenSpan.getStart, tokenSpan.getEnd)
    for {
      startSpan <- spans.headOption
      endSpan <- spans.lastOption
    } yield {
      val start = startSpan.getStart
      val end = endSpan.getEnd
      NameResult(
        start, end, sentence.slice(start, end)
      )
    }
  }
}
