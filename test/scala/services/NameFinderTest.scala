package services

import org.scalatest._

class NameFinderTest extends FlatSpec with Matchers {
  val nameFinder = new StanfordNameFinder

  "findNames" should "find names in the given text" in {
    val exampleText =
      "In an attempt to make a useful tool, Jonathon was trying out NLP."
    val expected = List(NameResult(37, 45, "Jonathon"))
    val actual = nameFinder.findNames(exampleText)
    expected should be(actual)
  }

  "findNames" should "correctly map names to sentences" in {
    val exampleText =
      "A sentence before the original sentence. In an attempt to make a useful tool, Jonathon was trying out NLP."
    val expected = List(NameResult(78, 86, "Jonathon"))
    val actual = nameFinder.findNames(exampleText)
    expected should be(actual)
  }

  "findNames" should "combine adjacent names" in {
    val exampleText =
      "The philosopher, Hubert Dreyfus, was smart, but also kindof crabby."
    val expected = List(NameResult(17, 31, "Hubert Dreyfus"))
    val actual = nameFinder.findNames(exampleText)
    expected should be(actual)
  }
}
