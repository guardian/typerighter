package services

import org.scalatest._
import opennlp.tools.namefind.TokenNameFinderModel
import opennlp.tools.sentdetect.SentenceModel
import opennlp.tools.tokenize.TokenizerModel

class NameFinderTest extends FlatSpec with Matchers {
  val nameFinderModelFile = getClass.getResourceAsStream("/openNLP/en-ner-person.bin")
  val tokenModelFile = getClass.getResourceAsStream("/openNLP/en-token.bin")
  val sentenceModelFile = getClass.getResourceAsStream("/openNLP/en-sent.bin")

  val nameFinderModel = new TokenNameFinderModel(nameFinderModelFile)
  val tokenModel = new TokenizerModel(tokenModelFile)
  val sentenceModel = new SentenceModel(sentenceModelFile)

  nameFinderModelFile.close()
  tokenModelFile.close()
  sentenceModelFile.close()

  "findNames" should "find names in the given text" in {

    val nameChecker = new NameFinder(nameFinderModel, tokenModel, sentenceModel)
    val exampleText =
      "In an attempt to make a useful tool, Jonathon was trying out NLP."
    val expected = List(NameResult(29, 37, "Jonathon"))
    val actual = nameChecker.findNames(exampleText)
    expected should be (actual)
  }
}
