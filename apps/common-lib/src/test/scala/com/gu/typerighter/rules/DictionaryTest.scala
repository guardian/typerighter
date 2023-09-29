package matchers

import com.gu.typerighter.rules.Dictionary.{dictionaryXmlToWordList, lemmaOrInflListToText}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DictionaryTest extends AnyFlatSpec with Matchers {
  val mockXml = <lemma_list language="english">
    <entry id="1"><lemma hyph="Type+righter">Typerighter</lemma></entry>
    <entry id="2"><lemma hyph="type+wri+ter">typewriter</lemma></entry>
    <entry id="3"><lemma hyph="type+wri+ter">typewriter</lemma><infl_list id="1.1" pos="n"><infl hyph="type+wri+ters">typewriters</infl></infl_list></entry>
  </lemma_list>

  "dictionaryXmlToWordList" should "find all unique words in a lemmatized dictionaryXml file" in {
    val expected = List("Typerighter", "typewriter", "typewriters")

    val actual = dictionaryXmlToWordList(mockXml)

    expected should be(actual)
  }

  "dictionaryXmlToWordList" should "not include strings that only contain whitespace" in {
    val node = <lemma_list language="english">
      <entry id="00099801">
        <lemma hyph="sto+ry">
          <!-- This node contains whitespace -->
        </lemma>
      </entry>
    </lemma_list>

    val expected = Nil

    val actual = dictionaryXmlToWordList(node)

    expected should be(actual)
  }

  "lemmaOrInflListToText" should "convert infl_list nodes into the text of their infl child nodes" in {
    val inflList =
      <infl_list><infl hyph="Type+righter">Typerighter</infl><infl hyph="type+writer">typewriter</infl></infl_list>
    val expected = List("Typerighter", "typewriter")

    val actual = lemmaOrInflListToText(inflList)

    expected should be(actual)
  }

  "lemmaOrInflListToText" should "convert unrelated nodes into Nil" in {
    val unrelatedNode = <typey>Typerighter</typey>
    val expected = Nil

    val actual = lemmaOrInflListToText(unrelatedNode)

    expected should be(actual)
  }
}
