package utils

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import xs4s.XMLStream

import scala.xml.Elem

class Dictionary extends AnyFlatSpec with Matchers {
  val mockXml: Elem = <lemma_list language="english">
    <entry id="1"><lemma hyph="Type+righter">Typerighter</lemma></entry>
    <entry id="2"><lemma hyph="type+wri+ter">typewriter</lemma></entry>
    <entry id="3"><lemma hyph="type+wri+ter">typewriter</lemma><infl_list id="1.1" pos="n"><infl hyph="type+wri+ters">typewriters</infl></infl_list></entry>
  </lemma_list>

  "dictionaryXmlToWordList" should "find all unique words in a lemmatized dictionaryXml file" in {
    val expected = List("Typerighter", "typewriter", "typewriters")

    val actual = Dictionary.lemmatisedListXmlToWordList(mockXml)

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

    val actual = Dictionary.lemmatisedListXmlToWordList(node)

    expected should be(actual)
  }

  "lemmaOrInflListToText" should "convert infl_list nodes into the text of their infl child nodes" in {
    val inflList =
      <infl_list><infl hyph="Type+righter">Typerighter</infl><infl hyph="type+writer">typewriter</infl></infl_list>
    val expected = List("Typerighter", "typewriter")

    val actual = Dictionary.lemmaOrInflListToText(inflList)

    expected should be(actual)
  }

  "lemmaOrInflListToText" should "only accept infl nodes within an infl_list" in {
    val inflList =
      <infl_list>
        <infl hyph="Type+righter">Typerighter</infl>
        <some_other_tag>This should not show up</some_other_tag>
      </infl_list>
    val expected = List("Typerighter")

    val actual = Dictionary.lemmaOrInflListToText(inflList)

    expected should be(actual)
  }

  "lemmaOrInflListToText" should "convert unrelated nodes into Nil" in {
    val unrelatedNode = <typey>Typerighter</typey>
    val expected = Nil

    val actual = Dictionary.lemmaOrInflListToText(unrelatedNode)

    expected should be(actual)
  }

  def getXml = XMLStream.fromFile(dictionaryTestStr)

  val entries = Dictionary.getDictionaryEntriesFromXml(getXml).toList
  val entriesMap = entries.map(entry => (entry.headword, entry)).toMap

  "getEntriesFromXml" should "parse entries" in {
    val actual = entriesMap.get("abacterial")
    val expected = Some(
      CollinsEntry(
        "abacterial",
        List(
          Definition(
            """not caused by or characterized by the presence of bacteria""",
            List(
              "Science_and_Technology",
              "Life_Sciences_and_Allied_Applications",
              "Biology"
            ),
            List()
          )
        ),
        Some("adjective")
      )
    )

    actual shouldBe expected
  }

  "Collins dictionary utilities" should "add inflections" in {
    val actual = entries
      .find(entry => entry.headword == "a" && entry.inflections.nonEmpty)
      .map(_.inflections)
      .getOrElse(Set.empty)

    val expected = Set("a's", "A's", "As")

    actual shouldBe expected
  }

  "Collins dictionary utilities" should "ensure subentries are distinct, but share definitions" in {
    val definitions = List(
      Definition(
        "ill at ease, embarrassed, or confused; ashamed",
        List("General", "General_Language", "General_Language_Term"),
        List()
      )
    )

    val mainEntry = entriesMap.get("abashed")
    val mainEntryExpected = Some(
      CollinsEntry(
        "abashed",
        definitions,
        Some("adjective")
      )
    )

    val subEntry = entriesMap.get("abashedly")
    val subEntryExpected = Some(
      CollinsEntry(
        "abashedly",
        definitions,
        Some("adjective")
      )
    )

    mainEntry shouldBe mainEntryExpected
    subEntry shouldBe subEntryExpected
  }

  "Collins dictionary utilities" should "ensure italic marks (<i>) are retained in the definition string" in {
    val actual = entriesMap.get("aardvark")
    val expected = Some(
      CollinsEntry(
        "aardvark",
        List(
          Definition(
            "a nocturnal mammal,<i>Orycteropus afer,</i>the sole member of its family (<i>Orycteropodidae</i>) and order (<i>Tubulidentata</i>). It inhabits the grasslands of Africa, has a long snout, and feeds on termites",
            List(
              "Science_and_Technology",
              "Life_Sciences_and_Allied_Applications",
              "Animals"
            ),
            List()
          )
        ),
        Some("noun")
      )
    )

    actual shouldBe expected
  }

  "Collins dictionary utilities" should "Creates a list of words, including inflections" in {
    val wordList = Dictionary.dictionaryXmlToWordList(getXml).toList.sortBy(identity)

    val expected = List(
      "3.5G",
      "401(k) plan",
      "4K",
      "5G",
      "A",
      "A & E",
      "A & M",
      "A & M college",
      "A & P",
      "A & R",
      "A game",
      "A road",
      "A'asia",
      "A's",
      "A-1",
      "A-one",
      "A-sample",
      "A-side",
      "A.",
      "A/A",
      "A1",
      "A2",
      "A2 level",
      "A3",
      "A4",
      "A5",
      "AA",
      "AAA",
      "AAM",
      "AAP",
      "AAPI",
      "AAR",
      "AARNet",
      "AARP",
      "AAU",
      "AAUP",
      "AAVE",
      "AB",
      "ABA",
      "Aachen",
      "Aadhaar",
      "Aadhar",
      "Aalborg",
      "Aalesund",
      "Aalst",
      "Aalto",
      "Aarau",
      "Aargau",
      "Aarhus",
      "Aaron",
      "Aaron's beard",
      "Aaron's rod",
      "Aaronic",
      "Ab",
      "Abadan",
      "Abaddon",
      "Abakan",
      "As",
      "a",
      "a'",
      "a's",
      "aa",
      "aah",
      "aal",
      "aalii",
      "aardvark",
      "aardwolf",
      "aardwolves",
      "aargh",
      "aarti",
      "aasvogel",
      "aba",
      "abac",
      "abaca",
      "abaci",
      "aback",
      "abacterial",
      "abactinal",
      "abactinally",
      "abactor",
      "abacus",
      "abaft",
      "abaka",
      "abalone",
      "abalone shell",
      "abampere",
      "aband",
      "abandon",
      "abandoned",
      "abandonedly",
      "abandonee",
      "abandoner",
      "abandonment",
      "abandonware",
      "abapical",
      "abase",
      "abasedly",
      "abasement",
      "abaser",
      "abash",
      "abashed",
      "abashedly",
      "abashment",
      "aw",
      "Å",
      "Ålborg",
      "Århus",
      "à bas"
    )
    wordList shouldBe expected
  }
}
