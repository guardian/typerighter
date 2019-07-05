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
    val sentenceTokens = sentences.map(tokenizer.tokenize)
    sentenceTokens.flatMap(sentence => {
      val spans = nameFinder.find(sentence)
      spans.map(extractPositionAndNameFromSpan(_, sentence))
    }).toList
  }

  private def extractPositionAndNameFromSpan(span: Span, sentence: Array[String]) = {
    val precedingText = sentence.slice(0, span.getStart).mkString
    val name = sentence.slice(span.getStart, span.getEnd).mkString
    NameResult(precedingText.length, precedingText.length + name.length, name)
  }
}
