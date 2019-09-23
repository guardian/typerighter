package services

import org.scalatest._
import utils.NameCheckerUtils

class NameCheckerFinderTest extends FlatSpec with Matchers {

  "findSimilarLastNames" should "return similar last names in the given list" in {
    val names = NameCheckerUtils.findSimilarLastNames(
      "Obama",
      List("Barack Obama", "Joe Biden", "Hilary Clinton", "Ira Magaziner")
    )
    names shouldBe List("Barack Obama")
  }

  "findSimilarLastNames" should "return similar last names when they're mispelled" in {
    val names = NameCheckerUtils.findSimilarLastNames(
      "Orama",
      List("Barack Obama", "Joe Biden", "Hilary Clinton", "Ira Magaziner")
    )
    names shouldBe List("Barack Obama")
  }

  "findSimilarLastNames" should "order results by similarity" in {
    val names = NameCheckerUtils.findSimilarLastNames(
      "Orama",
      List("Barack Obama", "Hussein Usama", "Hilary Clinton", "Ira Magaziner")
    )
    names shouldBe List("Barack Obama", "Hussein Usama")
  }
}
