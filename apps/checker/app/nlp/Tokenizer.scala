package services

import edu.stanford.nlp.ling.CoreAnnotations.{SentencesAnnotation, TextAnnotation, TokensAnnotation}
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.util.CoreMap

import java.util.Properties
import scala.jdk.CollectionConverters._

case class NameResult(from: Int, to: Int, text: String)

/**
  * A service to extract word tokens from text.
  */
class Tokenizer() {
  type WordToken =  (String, Int, Int)

  val props: Properties = new Properties()
  props.put("annotators", "tokenize, ssplit, pos")

  val pipeline: StanfordCoreNLP = new StanfordCoreNLP(props)

  def tokenize(text: String): List[WordToken] = {
    val annotation: Annotation = new Annotation(text)

    pipeline.annotate(annotation)

    val sentences: List[CoreMap] = annotation.get(classOf[SentencesAnnotation]).asScala.toList

    sentences.map { sentence =>
      sentence.get(classOf[TokensAnnotation])
        .asScala
        .toList
        .foldLeft(List.empty[WordToken])((acc, token) =>
          extractValidWordToken(token) match {
            case Some((word, _, to)) if (word.matches(".*['’].*")) =>
              val (prevWord, prevFrom, _) = acc.last
              acc.dropRight(1) :+ (prevWord + word, prevFrom, to)
            case Some(wordToken) =>
              acc :+ wordToken
            case None => acc
          }
        )
    }.flatten
  }

  def extractValidWordToken(token: CoreLabel) = {
    val word = token.get(classOf[TextAnnotation])

    if (TokenHelpers.containsWordCharacters(token) && TokenHelpers.doesNotContainNonWordToken(token)) {
      Some((word, token.beginPosition(), token.endPosition()))
    } else None
  }
}

