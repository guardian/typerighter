package services

import opennlp.tools.namefind.{NameFinderME, TokenNameFinderModel}
import opennlp.tools.sentdetect.{SentenceDetectorME, SentenceModel}
import opennlp.tools.tokenize.{TokenizerME, TokenizerModel}
import opennlp.tools.util.Span

case class NameResult(from: Int, to: Int, text: String)

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
      (tokenizer.tokenizePos(sentence), tokenizer.tokenize(sentence)))
    sentencesAsTokens.zipWithIndex.flatMap({
      case ((sentenceSpans, sentenceTokens), index) => {
        val spans = nameFinder.find(sentenceTokens)
        spans.map(extractPositionAndNameFromSpan(_, sentences(index), sentenceSpans))
      }
    }).toList.flatten
  }

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
