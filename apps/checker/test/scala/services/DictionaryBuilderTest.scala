package services

import com.gu.typerighter.model.{Category, DictionaryRule, TextBlock}
import matchers.DictionaryMatcher
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class DictionaryBuilderTest extends AsyncFlatSpec with Matchers {
  it should "build a dictionary from a list of words" in {
    val listOfWords = List("angry", "blue", "crab")

    val spellDictionary = new SpellDictionaryBuilder().buildDictionary(listOfWords)

    spellDictionary.exists() shouldBe true
  }

  it should "produce no matches when the words in the text blocks are found in the dictionary" in {
    val listOfWords = List("angry", "blue", "crab")
    new SpellDictionaryBuilder().buildDictionary(listOfWords)
    val dictionaryRules =
      listOfWords.map(word => DictionaryRule("123", word, Category("id", "desc")))
    val exampleTextBlocks = List(TextBlock("456", "angry blue crab", 0, 15))
    val exampleMatcherRequest = MatcherRequest(exampleTextBlocks)

    val matcher = new DictionaryMatcher(dictionaryRules)
    matcher
      .check(exampleMatcherRequest)
      .map(matches => {
        matches shouldBe List.empty
      })
  }

  it should "produce three matches when three words in a text block are not found in the dictionary" in {
    val listOfWords = List("angry", "blue", "crab")
    new SpellDictionaryBuilder().buildDictionary(listOfWords)
    val dictionaryRules =
      listOfWords.map(word => DictionaryRule("123", word, Category("id", "desc")))
    val exampleTextBlocks = List(TextBlock("456", "jolly red lobster", 0, 15))
    val exampleMatcherRequest = MatcherRequest(exampleTextBlocks)

    val matcher = new DictionaryMatcher(dictionaryRules)
    matcher
      .check(exampleMatcherRequest)
      .map(matches => {
        matches.length shouldBe 3
      })
  }
}
