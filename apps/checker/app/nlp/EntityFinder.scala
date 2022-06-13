package nlp

import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations.{NamedEntityTagAnnotation, LemmaAnnotation, PartOfSpeechAnnotation, SentencesAnnotation, TextAnnotation, TokensAnnotation}
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.util.CoreMap

import scala.collection.JavaConverters._

case class NameEntity(from: Int, to: Int, text: String)

/**
  * A service to extract proper names from documents.
  */
class EntityFinder() {
  val props: Properties = new Properties()
  props.put("annotators", "tokenize, ssplit, pos, lemma, ner")
  val pipeline: StanfordCoreNLP = new StanfordCoreNLP(props)

  def findNames(text: String): List[NameEntity] = {
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

    val (nameResults, _) = tokensAndEntities.foldLeft[(List[NameEntity], Option[(CoreLabel, String)])]((List.empty[NameEntity], None)) {
      case ((nameResults, maybeLastTokenAndWord), (token, word, entity)) if (entity == "PERSON") => {
        // If we have another token that represents a PERSON entity immediately
        // previous to this one, combine it. Assumption -- it seems very likely
        // that contiguous tokens identified as PERSON entities represent a name
        // comprised of  many tokens. (Punctuation tokens ensure that e.g. comma
        // separated lists of names are not flagged by this heuristic.)
        if (maybeLastTokenAndWord.isDefined) {
          val (lastToken, lastWord) = maybeLastTokenAndWord.get
          (nameResults.init :+ NameEntity(lastToken.beginPosition, token.endPosition, s"$lastWord $word"), Some((token, word)))
        } else {
          (nameResults :+ NameEntity(token.beginPosition, token.endPosition, word), Some((token, word)))
        }
      }
      case ((nameResults, maybeLastTokenAndWord), _) => (nameResults, None)
    }

    nameResults
  }
}

