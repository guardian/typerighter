package services

import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations.{SentencesAnnotation, TextAnnotation, TokensAnnotation}
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.util.CoreMap

import scala.collection.JavaConverters._

case class NameResult(from: Int, to: Int, text: String)

/**
  * A service to extract word tokens from text.
  */
class Tokenizer() {
  val props: Properties = new Properties()
  props.put("annotators", "tokenize, ssplit, pos")

  val pipeline: StanfordCoreNLP = new StanfordCoreNLP(props)

  def tokenize(text: String): List[(String, Int, Int)] = {
    val annotation: Annotation = new Annotation(text)

    pipeline.annotate(annotation)

    val sentences: List[CoreMap] = annotation.get(classOf[SentencesAnnotation]).asScala.toList

    for {
      sentence: CoreMap <- sentences
      token: CoreLabel <- sentence.get(classOf[TokensAnnotation]).asScala.toList
      word: String = token.get(classOf[TextAnnotation])
    } yield {
      (word, token.beginPosition(), token.endPosition())
    }
  }
}

