package db

import org.scalatest.flatspec.FixtureAnyFlatSpec
import scalikejdbc._
import scalikejdbc.DBSession
import scalikejdbc.scalatest.AutoRollback

trait RuleFixture extends FixtureAnyFlatSpec with AutoRollback {
  val autoSession = AutoSession

  override def fixture(implicit session: DBSession) = {
    val initialRevisionId = 0
    val initialExternalId = "googleSheetId"

    sql"""
      ALTER SEQUENCE rules_id_seq RESTART WITH 1;
      ALTER SEQUENCE tags_id_seq RESTART WITH 1;
    """.update().apply()

    val testRuleId =
      sql"insert into rules_draft (rule_type, pattern, replacement, category, description, ignore, notes, external_id, force_red_rule, advisory_rule, created_by, updated_by, rule_order) values (${"regex"}, ${"pattern"}, ${"replacement"}, ${"category"}, ${"description"}, false, ${"notes"}, ${"externalId"}, false, false, 'test.user', 'test.user', 1)"
        .updateAndReturnGeneratedKey()
        .apply()
        .toInt

    val testTagId = sql"insert into tags (name) values (${"testTag"})".update().apply()
    sql"insert into rule_tag_draft (rule_id, tag_id) values ($testRuleId, $testTagId)"
      .update()
      .apply()

    sql"insert into rules_live (rule_type, pattern, replacement, category, description, notes, external_id, force_red_rule, advisory_rule, created_by, updated_by, is_active, rule_order, revision_id) values ('regex', 'pattern', 'replacement', 'category', 'description', 'notes', $initialExternalId, false, false, 'test.user', 'test.user', true, 3, $initialRevisionId)"
      .update()
      .apply()

    sql"insert into rule_tag_live (rule_external_id, rule_revision_id, tag_id) values ($initialExternalId, $initialRevisionId, $testTagId)"
      .update()
      .apply()
  }

  def insertRule(
      ruleType: String = "regex",
      pattern: Option[String] = Some("Example"),
      description: Option[String] = None,
      tags: List[Int] = List.empty
  )(implicit session: DBSession = autoSession) = DbRuleDraft
    .create(
      ruleType = ruleType,
      pattern = pattern,
      description,
      description,
      user = "test.user",
      ignore = false,
      tags = tags
    )
    .get
}
