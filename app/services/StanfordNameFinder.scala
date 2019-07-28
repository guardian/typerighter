package services

import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations.{NamedEntityTagAnnotation, LemmaAnnotation, PartOfSpeechAnnotation, SentencesAnnotation, TextAnnotation, TokensAnnotation}
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.util.CoreMap

import scala.collection.JavaConverters._

case class NameResult(from: Int, to: Int, text: String)

/**
  * A service to extract proper names from documents.
  */
class StanfordNameFinder() {
  def findNames(text: String): List[NameResult] = {
    val props: Properties = new Properties()
    props.put("annotators", "tokenize, ssplit, pos, lemma, ner")

    val pipeline: StanfordCoreNLP = new StanfordCoreNLP(props)
    val document: Annotation = new Annotation(text)
    pipeline.annotate(document)

    val sentences: List[CoreMap] = document.get(classOf[SentencesAnnotation]).asScala.toList

    val tokensAndEntities = for {
      sentence: CoreMap <- sentences
      token: CoreLabel <- sentence.get(classOf[TokensAnnotation]).asScala.toList
      word: String = token.get(classOf[TextAnnotation])
      ner: String = token.get(classOf[NamedEntityTagAnnotation])
    } yield {
      (token, word, ner)
    }

    val (nameResults, _) = tokensAndEntities.foldLeft[(List[NameResult], Option[(CoreLabel, String)])]((List.empty[NameResult], None)) {
      case ((nameResults, maybeLastTokenAndWord), (token, word, entity)) => {
        if (entity == "PERSON") {
          // If we have another entity immediately previous to this one, combine it.
          if (maybeLastTokenAndWord.isDefined) {
            val (lastToken, lastWord) = maybeLastTokenAndWord.get
            (nameResults.init :+ NameResult(lastToken.beginPosition, token.endPosition, s"$lastWord $word"), Some((token, word)))
          } else {
            (nameResults :+ NameResult(token.beginPosition, token.endPosition, word), Some((token, word)))
          }
        } else (nameResults, None)
      }
    }

    nameResults
  }
}
