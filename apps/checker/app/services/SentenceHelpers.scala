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
  // These tokens can contain multiple quotes – we only need to detect the presence of one.
  val NON_WORD_TOKEN_CHARS = Set('`', '\'', '-')
  // These tokens are discrete. LSB == Left Square Bracket, etc.
  val NON_WORD_TOKENS = Set("-LSB-", "-LRB-", "-LCB-")
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
      tokenDoesNotContainNonWordToken(token) && tokenContainsWordCharacters(token)
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

  private def tokenDoesNotContainNonWordToken(token: CoreLabel) =
    !SentenceHelpers.NON_WORD_TOKENS.contains(token.value)

  private def tokenContainsWordCharacters(token: CoreLabel) = {
    val tokenWithNonWordCharsRemoved = token.value.filterNot(
      SentenceHelpers.NON_WORD_TOKEN_CHARS.contains
    )
    tokenWithNonWordCharsRemoved.size > 0
  }
}
