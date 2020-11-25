package matchers

import java.util.Properties

import model.{RuleMatch}
import services.MatcherRequest
import utils.{Matcher, MatcherCompanion, RuleMatchHelpers}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import model.TextRange
import model.NameRule
import edu.stanford.nlp.coref.CorefCoreAnnotations
import edu.stanford.nlp.coref.data.CorefChain
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation

import scala.collection.JavaConverters._

object NameMatcher extends MatcherCompanion {
  def getType() = "regex"
}

/**
  * A Matcher for rules based on regular expressions.
  */
class NameMatcher(rules: List[NameRule]) extends Matcher {
  val props = new Properties();
  props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,mention,coref");
  val pipeline = new StanfordCoreNLP(props);

  def getType() = NameMatcher.getType

  override def check(request: MatcherRequest)(implicit ec: ExecutionContext): Future[List[RuleMatch]] = {
    val results = request.blocks.map { block =>
      val doc = new Annotation(block.text)
      pipeline.annotate(doc)

      // Each chain represents a list of references that may include our name

      val sentences = doc.get(classOf[SentencesAnnotation]).asScala.toList
      val coreferenceChain = doc.get(classOf[CorefCoreAnnotations.CorefChainAnnotation]).asScala.values.toList.map { chain =>
        val mentions = chain.getMentionsInTextualOrder().asScala.toList.map { mention =>
          val sentence = sentences(mention.sentNum - 1)
          val tokens = sentence.get(classOf[TokensAnnotation]).asScala.toList
          val mentionTokens = tokens.slice(mention.startIndex - 1, mention.endIndex - 1)
          mentionTokens match {
            case Nil => (mention, None)
            case mentionTokens => (mention, Some(TextRange(mentionTokens.head.beginPosition(), mentionTokens.last.endPosition())))
          }
        }

        // We need to be confident that e.g. a chain that contains the words 'Sam' and 'Smith', maps to our name
        // Identify the parts of the chain that represent pronouns that we care about
        // CHECK – do the pronouns match the name?

        // --- IF NOT – resolve the positions of those pronouns in the sentence, and then the document

        // Provide a match with correct descriptions etc.
        mentions
      }

      println("coreference chains")
      coreferenceChain.foreach(x => println("\t" + x))
    }

    Future {
      List()
    }
  }

  override def getRules(): List[NameRule] = rules

  override def getCategories() = rules.map(_.category).toSet
}
