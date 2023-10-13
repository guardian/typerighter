package services

import com.gu.typerighter.model.TextRange
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffShouldMatcher._
import com.softwaremill.diffx.generic.auto._

class EntityHelperTest extends AsyncFlatSpec with Matchers {

  val entityHelper = new EntityHelper()

  behavior of "getEntitiesFromText"

  it should "return a list of person entities from the text" in {
    val entities = entityHelper.getEntitiesFromText(
      "The Beatles were an English rock band comprising John Lennon, Paul McCartney, George Harrison, and Ringo Starr."
    )

    entities shouldMatchTo (List(
      EntityInText("John", TextRange(49, 53)),
      EntityInText("Lennon", TextRange(54, 60)),
      EntityInText("Paul", TextRange(62, 66)),
      EntityInText("McCartney", TextRange(67, 76)),
      EntityInText("George", TextRange(78, 84)),
      EntityInText("Harrison", TextRange(85, 93)),
      EntityInText("Ringo", TextRange(99, 104)),
      EntityInText("Starr", TextRange(105, 110)),
      EntityInText("Beatles", TextRange(4, 11))
    ))
  }

  it should "return a list of organization entities from the text" in {
    val entities = entityHelper.getEntitiesFromText(
      "IBM made concessions to European Commission regulators in order to avoid a fine."
    )

    entities shouldMatchTo (List(
      EntityInText("IBM", TextRange(0, 3)),
      EntityInText("European", TextRange(24, 32)),
      EntityInText("Commission", TextRange(33, 43))
    ))
  }

  it should "return a list of location entities from the text" in {
    val entities = entityHelper.getEntitiesFromText(
      "The capital of Mozambique is Maputo."
    )

    entities shouldMatchTo (List(
      EntityInText("Mozambique", TextRange(15, 25)),
      EntityInText("Maputo", TextRange(29, 35))
    ))
  }
}
