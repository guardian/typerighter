package db

import java.time.ZonedDateTime

trait DbRule {
  def id: Option[Int]
  def ruleType: String
  def pattern: Option[String]
  def replacement: Option[String]
  def category: Option[String]
  def tags: Option[String]
  def description: Option[String]
  def notes: Option[String]
  def googleSheetId: Option[String]
  def forceRedRule: Option[Boolean]
  def advisoryRule: Option[Boolean]
  def createdAt: ZonedDateTime
  def createdBy: String
  def updatedAt: ZonedDateTime
  def updatedBy: String
  def revisionId: Int
}

object DbRuleHelpers {
  val dbColumns: Seq[String] = Seq(
    "id",
    "rule_type",
    "pattern",
    "replacement",
    "category",
    "tags",
    "description",
    "notes",
    "google_sheet_id",
    "force_red_rule",
    "advisory_rule",
    "created_at",
    "created_by",
    "updated_at",
    "updated_by",
    "revision_id"
  )
}
