package matchers

import java.util.Properties

import model.{NameRule, Pronoun, RuleMatch, TextRange}
import services.MatcherRequest
import utils.{Matcher, MatcherCompanion, RuleMatchHelpers}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import edu.stanford.nlp.coref.CorefCoreAnnotations
import edu.stanford.nlp.coref.data.CorefChain
import edu.stanford.nlp.coref.data.Dictionaries.MentionType
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation
import edu.stanford.nlp.util.CoreMap

import scala.collection.JavaConverters._


object NameMatcher extends MatcherCompanion {
  def getType() = "name"
}

/**
  * Check that the subject of the chain matches the name of our rule
  *   We need to be confident that e.g. a chain that contains the words 'Sam' and 'Smith', maps to our name
  *
  *   Identify the parts of the chain that represent pronouns that we care about
  *   CHECK – do the pronouns match the name?
  *
  *   --- IF NOT – resolve the positions of those pronouns in the sentence, and then the document
  *
  *   Provide a match with correct descriptions etc.
  */
class NameMatcher(rules: List[NameRule]) extends Matcher {
  val props = new Properties();
  props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,mention,coref");
  val pipeline = new StanfordCoreNLP(props);

  def getType() = NameMatcher.getType

  override def check(request: MatcherRequest)(implicit ec: ExecutionContext): Future[List[RuleMatch]] = {
    val results = request.blocks.foldLeft(List.empty[RuleMatch])((accBlock, block) => {
      val doc = new Annotation(block.text)
      pipeline.annotate(doc)

      val sentences = doc.get(classOf[SentencesAnnotation]).asScala.toList

      // Each chain represents a list of references that are related
      val matches = doc.get(classOf[CorefCoreAnnotations.CorefChainAnnotation]).asScala.values.toList.foldLeft(List.empty[RuleMatch])((accChain, chain) => {

        // Create a helper class to add some extra props
        val nameCheckerChain = new NameCheckerCorefChain(chain)(sentences)

        println(nameCheckerChain)

        if (nameCheckerChain.chainContainsPronouns) {
          println("\tChecking rules for chain")
          // Loop through our rules and get any matches
          val matches = rules.foldLeft(List.empty[RuleMatch])((accRule, rule) => {
            val matches = nameCheckerChain.check(rule)
            RuleMatchHelpers.removeOverlappingRules(accRule, matches) ++ matches
          })

          RuleMatchHelpers.removeOverlappingRules(accChain, matches) ++ matches
        } else {
          println("\tSkipping chain as it contains no pronouns\n")
          accChain
        }

      })

      RuleMatchHelpers.removeOverlappingRules(accBlock, matches) ++ matches
    })

    Future {
      results
    }
  }

  override def getRules(): List[NameRule] = rules

  override def getCategories() = rules.map(_.category).toSet
}

class NameCheckerCorefChain(chain: CorefChain)(sentences: List[CoreMap]) {
  val id: Int = chain.getChainID

  val subject: String = chain.getRepresentativeMention.mentionSpan

  // Get all mentions in the chain along with their position in the block and whether they are pronounial
  val mentions: Seq[NameMatcherCorefMention] = chain.getMentionsInTextualOrder.asScala.toList.map { mention =>
    new NameMatcherCorefMention(mention)(sentences)
  }

  val chainContainsPronouns: Boolean = mentions.exists(mention => mention.pronounial)

  def check(rule:NameRule): List[RuleMatch] = {
    println(s"\t\tRule: ${rule.fullName}")
    val chainRefersToName = checkChainRefersToName(rule)

    if (!chainRefersToName) {
      println(s"\t\tSkipping rule as chain doesn't refer to name")
      List.empty[RuleMatch]
    } else {
      println(s"\t\tChain refers to name. Checking pronouns.")
      val matches: List[RuleMatch] = List.empty[RuleMatch]

      mentions.filter(m => m.pronounial).foreach(m => {
        checkPronounIsCorrect(m, rule.pronoun)
      })

      matches
    }
  }

  def checkChainRefersToName(rule: NameRule): Boolean = {
    if (subject == rule.firstName || subject == rule.lastName || subject == rule.fullName) {
      true
    } else {
      false
    }
  }

  def checkPronounIsCorrect(mention: NameMatcherCorefMention, pronoun: Pronoun): Boolean = {
    println(s"Text: ${mention.text} Pronoun: ${pronoun}")
    mention
    true
  }

  override def toString: String = {
    s"\tID: $id\n\tSubject: $subject\n\tMentions: $mentions\n\tContains Pronouns: $chainContainsPronouns\n"
  }
}

class NameMatcherCorefMention(mention: CorefChain.CorefMention)(sentences: List[CoreMap]) {
  val text: String = mention.mentionSpan
  val pronounial: Boolean = mention.mentionType == MentionType.PRONOMINAL
  val (textRange, tags) = getTokenData

  private def getTokenData: (Option[TextRange], Option[List[String]]) = {
    val sentence = sentences(mention.sentNum - 1)
    val tokens = sentence.get(classOf[TokensAnnotation]).asScala.toList
    val mentionTokens = tokens.slice(mention.startIndex - 1, mention.endIndex - 1)

    mentionTokens match {
      case Nil => (None, None)
      case mentionTokens => (Some(TextRange(mentionTokens.head.beginPosition(), mentionTokens.last.endPosition())), Some(mentionTokens.map(_.tag)))
    }
  }

  override def toString: String = {
    s"${mention.toString} - Pronounial: $pronounial - Tags: $tags"
  }
}