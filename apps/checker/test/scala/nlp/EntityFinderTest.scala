package nlp

import nlp.EntityFinder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EntityFinderTest extends AnyFlatSpec with Matchers {
  val entityFinder = new EntityFinder()

  behavior of "findNames"

  it should "find names in the given text" in {
    val exampleText =
      "In an attempt to make a useful tool, Jonathon was trying out NLP."
    val expected = List(NameEntity(37, 45, "Jonathon"))
    val actual = entityFinder.findNames(exampleText)
    expected should be(actual)
  }

  it should "correctly map names to sentences" in {
    val exampleText =
      "A sentence before the original sentence. In an attempt to make a useful tool, Jonathon was trying out NLP."
    val expected = List(NameEntity(78, 86, "Jonathon"))
    val actual = entityFinder.findNames(exampleText)
    expected should be(actual)
  }

  it should "combine adjacent names" in {
    val exampleText =
      "The philosopher, Hubert Dreyfus, was smart, but also kindof crabby."
    val expected = List(NameEntity(17, 31, "Hubert Dreyfus"))
    val actual = entityFinder.findNames(exampleText)
    expected should be(actual)
  }

  it should "combine adjacent names without conflating them" in {
    val exampleText =
      "Hubert Dreyfus was a huge fan of Ludwig Wittgenstein, Immaneul Kant, and Martin Heidegger"

    val expected = List(
      NameEntity(0,14,"Hubert Dreyfus"),
      NameEntity(33,52,"Ludwig Wittgenstein"),
      NameEntity(54,67,"Immaneul Kant"),
      NameEntity(73,89,"Martin Heidegger")
    )
    val actual = entityFinder.findNames(exampleText)

    expected should be(actual)
  }
}
