package db

import java.time.ZonedDateTime

trait DbRuleFields {
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

trait LiveDbRuleFields {
  def reason: String
}
