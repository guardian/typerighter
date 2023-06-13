package db

object RuleTagDraft extends RuleTagCommon {
  override val columns = Seq("rule_id", "tag_id")
  override val tableName = "rule_tag_draft"
}
