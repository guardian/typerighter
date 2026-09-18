package db

import models.{MatchContext, UserFeedbackWithEmail}
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback

class UserFeedbackSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {

  behavior of "UserFeedback"

  it should "create a record without match context" in { implicit session =>
    val result = UserFeedback.create(
      UserFeedbackWithEmail(
        app = "composer",
        stage = "PROD",
        documentUrl = "https://example.com/doc/1",
        feedbackMessage = "This rule is wrong",
        userEmail = "user@example.com",
        matchContext = None
      )
    )

    result.isSuccess should be(true)
    val feedback = result.getOrElse(fail("Create method did not return UserFeedback instance"))
    feedback.id.isDefined should be(true)
    feedback.app should be("composer")
    feedback.stage should be("PROD")
    feedback.documentUrl should be("https://example.com/doc/1")
    feedback.feedbackMessage should be("This rule is wrong")
    feedback.userEmail should be("user@example.com")
    feedback.matchId should be(None)
    feedback.externalRuleId should be(None)
    feedback.ruleId should be(None)
    feedback.documentId should be(None)
    feedback.matcherType should be(None)
    feedback.suggestion should be(None)
    feedback.matchIsMarkedAsCorrect should be(None)
    feedback.matchIsAdvisory should be(None)
    feedback.matchHasReplacement should be(None)
    feedback.matchedText should be(None)
    feedback.matchContext should be(None)
  }

  it should "create a record with match context" in { implicit session =>
    val result = UserFeedback.create(
      UserFeedbackWithEmail(
        app = "composer",
        stage = "PROD",
        documentUrl = "https://example.com/doc/1",
        feedbackMessage = "False positive",
        userEmail = "user@example.com",
        matchContext = Some(
          MatchContext(
            matchId = "match-123",
            ruleId = "rule-456",
            documentId = "doc-789",
            matcherType = "regex",
            suggestion = Some("replacement text"),
            matchIsMarkedAsCorrect = false,
            matchIsAdvisory = true,
            matchHasReplacement = true,
            matchedText = "teh",
            matchContext = "I wrote teh wrong word"
          )
        )
      )
    )

    result.isSuccess should be(true)
    val feedback = result.getOrElse(fail("UserFeedback should exist"))

    feedback.matchId should be(Some("match-123"))
    feedback.externalRuleId should be(Some("rule-456"))
    feedback.ruleId should be(None)
    feedback.documentId should be(Some("doc-789"))
    feedback.matcherType should be(Some("regex"))
    feedback.suggestion should be(Some("replacement text"))
    feedback.matchIsMarkedAsCorrect should be(Some(false))
    feedback.matchIsAdvisory should be(Some(true))
    feedback.matchHasReplacement should be(Some(true))
    feedback.matchedText should be(Some("teh"))
    feedback.matchContext should be(Some("I wrote teh wrong word"))
  }

  it should "find a record by id" in { implicit session =>
    val created = UserFeedback
      .create(
        UserFeedbackWithEmail(
          app = "composer",
          stage = "CODE",
          documentUrl = "https://example.com/doc/2",
          feedbackMessage = "Test feedback",
          userEmail = "finder@example.com",
          matchContext = None
        )
      )
      .getOrElse(fail("Failed to create UserFeedback record"))

    val found = UserFeedback
      .find(created.id.getOrElse(fail("No id on the returned UserFeedback instances")))
      .getOrElse(fail("No UserFeedback found with the specified id"))
    found.feedbackMessage should be("Test feedback")
    found.userEmail should be("finder@example.com")
  }

  it should "return None when finding a non-existent record" in { implicit session =>
    val found = UserFeedback.find(99999)
    found should be(None)
  }

  it should "find all records ordered by created_at descending" in { implicit session =>
    UserFeedback.create(
      UserFeedbackWithEmail(
        app = "composer",
        stage = "PROD",
        documentUrl = "https://example.com/doc/1",
        feedbackMessage = "First feedback",
        userEmail = "user@example.com",
        matchContext = None
      )
    )

    UserFeedback.create(
      UserFeedbackWithEmail(
        app = "composer",
        stage = "PROD",
        documentUrl = "https://example.com/doc/2",
        feedbackMessage = "Second feedback",
        userEmail = "user@example.com",
        matchContext = None
      )
    )

    val all = UserFeedback.findAll()
    all.size should be >= 2
    all.head.feedbackMessage should be("Second feedback")
    all(1).feedbackMessage should be("First feedback")
  }

  it should "populate ruleId from a matching DbRuleDraft when externalRuleId is provided" in {
    implicit session =>
      val draftRule = DbRuleDraft
        .create(
          ruleType = "regex",
          pattern = Some("teh"),
          replacement = Some("the"),
          ignore = false,
          externalId = Some("ext-rule-1"),
          user = "test@example.com"
        )
        .getOrElse(fail("Failed to create draft rule"))

      val result = UserFeedback.create(
        UserFeedbackWithEmail(
          app = "composer",
          stage = "PROD",
          documentUrl = "https://example.com/doc/5",
          feedbackMessage = "Matched a known rule",
          userEmail = "user@example.com",
          matchContext = Some(
            MatchContext(
              matchId = "match-500",
              ruleId = "ext-rule-1",
              documentId = "doc-500",
              matcherType = "regex",
              suggestion = Some("the"),
              matchIsMarkedAsCorrect = false,
              matchIsAdvisory = false,
              matchHasReplacement = true,
              matchedText = "teh",
              matchContext = "I wrote teh wrong word"
            )
          )
        )
      )

      result.isSuccess should be(true)
      val feedback = result.getOrElse(fail("UserFeedback should exist"))
      feedback.externalRuleId should be(Some("ext-rule-1"))
      feedback.ruleId should be(draftRule.id)
  }

  it should "leave ruleId as None when externalRuleId does not match any DbRuleDraft" in {
    implicit session =>
      val result = UserFeedback.create(
        UserFeedbackWithEmail(
          app = "composer",
          stage = "PROD",
          documentUrl = "https://example.com/doc/6",
          feedbackMessage = "Unknown rule",
          userEmail = "user@example.com",
          matchContext = Some(
            MatchContext(
              matchId = "match-600",
              ruleId = "nonexistent-rule",
              documentId = "doc-600",
              matcherType = "regex",
              suggestion = None,
              matchIsMarkedAsCorrect = false,
              matchIsAdvisory = false,
              matchHasReplacement = false,
              matchedText = "foo",
              matchContext = "some foo context"
            )
          )
        )
      )

      result.isSuccess should be(true)
      val feedback = result.getOrElse(fail("UserFeedback should exist"))
      feedback.externalRuleId should be(Some("nonexistent-rule"))
      feedback.ruleId should be(None)
  }

  it should "convert from UserFeedbackWithAuthentication with match context" in { () =>
    val matchCtx = MatchContext(
      matchId = "match-1",
      ruleId = "rule-1",
      documentId = "doc-1",
      matcherType = "regex",
      suggestion = Some("suggested"),
      matchIsMarkedAsCorrect = true,
      matchIsAdvisory = false,
      matchHasReplacement = true,
      matchedText = "teh",
      matchContext = "I wrote teh word"
    )

    val authenticated = UserFeedbackWithEmail(
      app = "composer",
      stage = "PROD",
      documentUrl = "https://example.com/doc/3",
      feedbackMessage = "Bad match",
      userEmail = "auth@example.com",
      matchContext = Some(matchCtx)
    )

    val feedback = UserFeedback.fromUserFeedbackWithAuthentication(authenticated)
    feedback.id should be(None)
    feedback.app should be("composer")
    feedback.userEmail should be("auth@example.com")
    feedback.matchId should be(Some("match-1"))
    feedback.externalRuleId should be(Some("rule-1"))
    feedback.ruleId should be(None)
    feedback.documentId should be(Some("doc-1"))
    feedback.matcherType should be(Some("regex"))
    feedback.suggestion should be(Some("suggested"))
    feedback.matchIsMarkedAsCorrect should be(Some(true))
    feedback.matchIsAdvisory should be(Some(false))
    feedback.matchHasReplacement should be(Some(true))
    feedback.matchedText should be(Some("teh"))
    feedback.matchContext should be(Some("I wrote teh word"))
  }

  it should "convert from UserFeedbackWithAuthentication without match context" in { () =>
    val authenticated = UserFeedbackWithEmail(
      app = "composer",
      stage = "CODE",
      documentUrl = "https://example.com/doc/4",
      feedbackMessage = "No context",
      userEmail = "nocontext@example.com",
      matchContext = None
    )

    val feedback = UserFeedback.fromUserFeedbackWithAuthentication(authenticated)
    feedback.id should be(None)
    feedback.app should be("composer")
    feedback.matchId should be(None)
    feedback.externalRuleId should be(None)
    feedback.ruleId should be(None)
    feedback.documentId should be(None)
    feedback.matcherType should be(None)
    feedback.suggestion should be(None)
    feedback.matchIsMarkedAsCorrect should be(None)
    feedback.matchIsAdvisory should be(None)
    feedback.matchHasReplacement should be(None)
    feedback.matchedText should be(None)
    feedback.matchContext should be(None)
  }
}
