package scala.model

import model._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffMatcher._

import utils.Text

class TextRangeTest extends AsyncFlatSpec with Matchers {
  behavior of "mapAddedRange"

  it should "account for a range added before the given range" in {
    val incomingRange = TextRange(10, 15)
    val addedRange = TextRange(0, 5)
    incomingRange.mapAddedRange(addedRange) shouldBe TextRange(15, 20)
  }

  it should "account for a range added within the given range" in {
    val incomingRange = TextRange(10, 15)
    val addedRange = TextRange(10, 15)
    incomingRange.mapAddedRange(addedRange) shouldBe TextRange(15, 20)
  }

  it should "account for a range added within the given range" in {
    val incomingRange = TextRange(15, 20)
    val addedRange = TextRange(10, 15)
    incomingRange.mapAddedRange(addedRange) shouldBe TextRange(10, 15)
  }

  it should "account for a range added partially within the given range – left hand side" in {
    val incomingRange = TextRange(10, 15)
    val addedRange = TextRange(5, 12)
    incomingRange.mapAddedRange(addedRange) shouldBe TextRange(17, 22)
  }

  it should "account for a range added partially the given range – right hand side" in {
    val incomingRange = TextRange(10, 15)
    val addedRange = TextRange(13, 20)
    incomingRange.mapAddedRange(addedRange) shouldBe TextRange(10, 22)
  }

  behavior of "mapRemovedRange"

  it should "account for a range removed before the given range" in {
    val incomingRange = TextRange(10, 15)
    val removedRange = TextRange(0, 5)
    incomingRange.mapRemovedRange(removedRange) shouldBe TextRange(5, 10)
  }

  it should "account for a range completely removed within the given range" in {
    val incomingRange = TextRange(10, 15)
    val removedRange = TextRange(10, 15)
    incomingRange.mapRemovedRange(removedRange) shouldBe TextRange(10, 10)
  }

  it should "account for a range partially removed within the given range – left hand side" in {
    val incomingRange = TextRange(10, 15)
    val removedRange = TextRange(5, 12)
    incomingRange.mapRemovedRange(removedRange) shouldBe TextRange(5, 8)
  }

  it should "account for a range partially within the given range – right hand side" in {
    val incomingRange = TextRange(10, 15)
    val removedRange = TextRange(13, 20)
    incomingRange.mapRemovedRange(removedRange) shouldBe TextRange(10, 13)
  }

  behavior of "getIntersectionOfRanges"

  it should "return an option containing a new range representing the intersection of two ranges" in {
    val rangeA = TextRange(0, 5)
    val rangeB = TextRange(4, 6)
    rangeA.getIntersection(rangeB) shouldBe Some(TextRange(4, 5))
  }

  it should "not return a range if there is no intersection" in {
    val rangeA = TextRange(0, 3)
    val rangeB = TextRange(4, 6)
    rangeA.getIntersection(rangeB) shouldBe None
  }
}
