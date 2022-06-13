package nlp

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DistanceHelpersTest extends AnyFlatSpec with Matchers {

  behavior of "findSimilarNames"

  it should "return similar last names in the given list" in {
    val names = DistanceHelpers.findSimilarNames(
      "Barack Obama",
      Set("Barack Obama", "Joe Biden", "Hilary Clinton", "Ira Magaziner")
    )
    names shouldBe List("Barack Obama")
  }

  it should "return similar last names when they're mispelled" in {
    val names = DistanceHelpers.findSimilarNames(
      "Barack Orama",
      Set("Barack Obama", "Joe Biden", "Hilary Clinton", "Ira Magaziner")
    )
    names shouldBe List("Barack Obama")
  }

  it should "order results by similarity" in {
    val names = DistanceHelpers.findSimilarNames(
      "Rachel Weitz",
      Set("Rachel Weisz", "Rachel White")
    )
    names shouldBe List("Rachel Weisz", "Rachel White")
  }
}
