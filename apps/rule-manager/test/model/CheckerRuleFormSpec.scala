package model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.data.FormError

class RuleManagerSpec extends AnyFlatSpec with Matchers {
  behavior of "CheckerRuleForm"

  it should "accept valid regexes" in {
    val result = RegexRuleForm.form.fillAndValidate(
      ("validRegex", None, "category", "description", "externalId")
    )
    result.errors shouldBe Nil
  }

  it should "reject invalid regexes" in {
    val result = RegexRuleForm.form.fillAndValidate(
      ("(invalidRegex", None, "category", "description", "externalId")
    )
    result.errors shouldBe Seq(
      FormError(
        "pattern",
        List("Error parsing the regular expression: Unclosed group near index 13\n(invalidRegex")
      )
    )
  }

  it should "accept valid xml" in {
    val result = LTRuleXMLForm.form.fillAndValidate(
      ("<rulegroup id=\"VS\" name=\"vs\"></rulegroup>", "category", "description", "externalId")
    )
    result.errors shouldBe Nil
  }

  it should "reject invalid xml" in {
    val result = LTRuleXMLForm.form.fillAndValidate(
      ("<rulegroup id=\"VS\" name=\"vs\">", "category", "description", "externalId")
    )
    result.errors shouldBe Seq(
      FormError(
        "pattern",
        List(
          "Error parsing the XML: XML document structures must start and end within the same entity."
        )
      )
    )
  }
}
