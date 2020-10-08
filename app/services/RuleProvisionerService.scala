package services

import java.util.Date

import akka.actor.Scheduler
import matchers.RegexMatcher
import play.api.Logging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import model.{RegexRule, BaseRule, Category, RuleResource}
import rules.BucketRuleManager
import matchers.LanguageToolFactory
import model.LTRule
import model.LTRuleXML

class RuleProvisionerService(
  bucketRuleManager: BucketRuleManager,
  matcherPool: MatcherPool,
  languageToolFactory: LanguageToolFactory
)(implicit ec: ExecutionContext) extends Logging with Runnable {

  var lastModified: Date = new Date(0)

  /**
    * Update the rules in our matcherPool, given a ruleResource.
    */
  def updateRules(ruleResource: RuleResource, date: Date): Unit = {
    matcherPool.removeAllMatchers()

    ruleResource.rules.groupBy(_.category).foreach {
      case (_, rules) => {
        val regexRules = rules.collect { case r: RegexRule => r }
        if (regexRules.size > 0) {
          val regexMatcher = new RegexMatcher(regexRules)
          matcherPool.addMatcher(regexMatcher)
        }

        val ltRules = rules.collect { case r: LTRuleXML => r }
        if (ltRules.size > 0) {
          addLTMatcherToPool(matcherPool, ltRules)
        }
      }
    }

    addLTMatcherToPool(matcherPool, Nil, ruleResource.ltDefaultRuleIds)

    lastModified = date
  }

  /**
    * Update our matcherPool rules from the S3 bucket.
    */
  def updateRulesFromBucket(): Unit = {
    bucketRuleManager.getRules.map {
      case (ruleResource, date) => {
        updateRules(ruleResource, date)
      }
    }
  }

  /**
    * Update our matcherPool rules from the S3 bucket,
    * if what's in the bucket is newer that what's in memory.
    */
  def maybeUpdateRulesFromBucket(): Unit = {
    bucketRuleManager.getRulesLastModified match {
      case Right(date) if date.compareTo(lastModified) > 0 => updateRulesFromBucket
      case Right(_) => logger.info("No rule update needed")
      case Left(error) => logger.error("Could not get last modified from S3")
    }
  }

  override def run(): Unit = maybeUpdateRulesFromBucket

  def scheduleUpdateRules(scheduler: Scheduler): Unit = {
    scheduler.scheduleWithFixedDelay(0.seconds, 1.minute)(this)
  }

  private def addLTMatcherToPool(matcherPool: MatcherPool, xmlRules: List[LTRuleXML], defaultRules: List[String] = Nil) = {
    languageToolFactory.createInstance(xmlRules, defaultRules) match {
      case Right(matcher) => matcherPool.addMatcher(matcher)
      case Left(errors) => {
        logger.error(s"Could not create languageTool instance from ruleResource: ${errors.size} errors found")
        errors.foreach(e => logger.error(e.getMessage(), e))
      }
    }
  }
}
