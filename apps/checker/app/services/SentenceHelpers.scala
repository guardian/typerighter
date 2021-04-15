package services

import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations.{SentencesAnnotation, TextAnnotation, TokensAnnotation}
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.util.CoreMap

import scala.collection.JavaConverters._
import model.TextRange

case class WordInSentence(sentence: String, word: String, range: TextRange)

object SentenceHelpers {
  // LSB == Left square bracket, etc.
  val NON_WORD_TOKENS = List("`", "``", "-LSB-", "-LRB-", "-LCB-")
}

/**
  * A service to extract proper names from documents.
  */
class SentenceHelpers() {
  val props: Properties = new Properties()
  props.put("annotators", "tokenize, ssplit")
  val pipeline: StanfordCoreNLP = new StanfordCoreNLP(props)

  def getFirstWordsInSentences(text: String): List[WordInSentence] = {
    val document: Annotation = new Annotation(text)
    pipeline.annotate(document)

    val sentences: List[CoreMap] = document.get(classOf[SentencesAnnotation]).asScala.toList

    for {
      sentence: CoreMap <- sentences
      wordInSentence <- maybeGetFirstWordFromSentence(sentence)
    } yield wordInSentence
  }

  def maybeGetFirstWordFromSentence(sentence: CoreMap) = {
    val tokens = sentence.get(classOf[TokensAnnotation]).asScala.toList
    val maybeFirstValidToken = tokens.find { token =>
      !SentenceHelpers.NON_WORD_TOKENS.contains(token.toString())
    }

    maybeFirstValidToken.map { token =>
      val word = token.get(classOf[TextAnnotation])
      WordInSentence(
        sentence.toString,
        word,
        TextRange(token.beginPosition(), token.endPosition())
      )
    }
  }
}
