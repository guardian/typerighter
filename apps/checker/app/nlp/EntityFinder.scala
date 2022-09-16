package nlp

import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations.{NamedEntityTagAnnotation, SentencesAnnotation, TextAnnotation, TokensAnnotation}
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.util.CoreMap

import scala.collection.JavaConverters._

sealed trait Entity {
  val from: Int
  val to: Int
  val text: String
}
case class NameEntity(from: Int, to: Int, text: String) extends Entity
case class NonWordEntity(from: Int, to: Int, text: String) extends Entity

/**
  * A service to extract proper names from documents.
  */
class EntityFinder() {
  // https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address
  private val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""
  private val twitterHandleRegex = "@.*"
  private val hashtagRegex = "#.*"
  private val nonWordTokenChecks = List(emailRegex, twitterHandleRegex, hashtagRegex)

  val props: Properties = new Properties()
  props.put("annotators", "tokenize, ssplit, pos, lemma, ner")
  val pipeline: StanfordCoreNLP = new StanfordCoreNLP(props)

  /**
    * Find names and non-word tokens (e.g. twitter handles, e-mail addresses).
    */
  def findNamesAndNonWordTokens(text: String, offset: Int = 0): List[Entity] = {
    // Strip some chars that aren't useful when detecting names:
    // - '/' is sometimes used in captions (e.g. "David Levene/The Guardian"),
    //   and results in "Levene/The" being treated as a single, non-entity token.
    val cleanedText = text.replaceAll("/", " ")
    val document: Annotation = new Annotation(cleanedText)
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

    val (nameResults, _) = tokensAndEntities.foldLeft[(List[Entity], Option[(CoreLabel, String)])]((List.empty[Entity], None)) {
      case ((nameResults, maybeLastTokenAndWord), (token, word, entity)) if entity == "PERSON" =>
        // If we have another token that represents a PERSON entity immediately
        // previous to this one, combine it. Assumption -- it seems very likely
        // that contiguous tokens identified as PERSON entities represent a name
        // comprised of  many tokens. (Punctuation tokens ensure that e.g. comma
        // separated lists of names are not flagged by this heuristic.)
        if (maybeLastTokenAndWord.isDefined) {
          val (lastToken, lastWord) = maybeLastTokenAndWord.get
          (nameResults.init :+ NameEntity(lastToken.beginPosition + offset, token.endPosition + offset, s"$lastWord $word"), Some((token, word)))
        } else {
          (nameResults :+ NameEntity(token.beginPosition + offset, token.endPosition + offset, word), Some((token, word)))
        }
      case ((nameResults, _), (token, word, _)) =>
        if (nonWordTokenChecks.exists(word.matches)) {
          (nameResults :+ NonWordEntity(token.beginPosition + offset, token.endPosition + offset, word), None)
        } else (nameResults, None)
    }

    nameResults
  }
}

