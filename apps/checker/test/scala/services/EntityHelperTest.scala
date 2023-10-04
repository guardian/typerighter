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
      "The Beatles were an English rock band comprising John Lennon, Paul McCartney, George Harrison, and Ringo Starr.",
      model = "person"
    )

    entities shouldMatchTo (List(
      EntityInText("John", TextRange(49, 53), "person"),
      EntityInText("Lennon", TextRange(54, 60), "person"),
      EntityInText("Paul", TextRange(62, 66), "person"),
      EntityInText("McCartney", TextRange(67, 76), "person"),
      EntityInText("George", TextRange(78, 84), "person"),
      EntityInText("Harrison", TextRange(85, 93), "person"),
      EntityInText("Ringo", TextRange(99, 104), "person"),
      EntityInText("Starr", TextRange(105, 110), "person")
    ))
  }

  it should "return a list of organization entities from the text" in {
    val entities = entityHelper.getEntitiesFromText(
      "IBM made concessions to European Commission regulators in order to avoid a fine.",
      model = "organization"
    )

    entities shouldMatchTo (List(
      EntityInText("IBM", TextRange(0, 3), "organization"),
      EntityInText("European", TextRange(24, 32), "organization"),
      EntityInText("Commission", TextRange(33, 43), "organization")
    ))
  }

  it should "return a list of location entities from the text" in {
    val entities = entityHelper.getEntitiesFromText(
      "The capital of Mozambique is Maputo.",
      model = "location"
    )

    entities shouldMatchTo (List(
      EntityInText("Mozambique", TextRange(15, 25), "location"),
      EntityInText("Maputo", TextRange(29, 35), "location")
    ))
  }
}
