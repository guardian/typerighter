package services

import java.util.Date

import akka.actor.Scheduler
import com.gu.typerighter.model.{BaseRule, Category, LTRule, LTRuleXML, RegexRule, RuleResource}
import com.gu.typerighter.rules.BucketRuleManager
import matchers.RegexMatcher
import play.api.Logging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import matchers.LanguageToolFactory
import utils.CloudWatchClient
import utils.Metrics

class RuleProvisionerService(
  bucketRuleManager: BucketRuleManager,
  matcherPool: MatcherPool,
  languageToolFactory: LanguageToolFactory,
  cloudWatchClient: CloudWatchClient
)(implicit ec: ExecutionContext) extends Logging with Runnable {

  var lastModified: Date = new Date(0)

  /**
    * Update the rules in our matcherPool, given a ruleResource.
    */
  def updateRules(ruleResource: RuleResource, date: Date): Either[List[Throwable], Unit] = {
    matcherPool.removeAllMatchers()

    val defaultRulesErrors = addLTMatcherToPool(matcherPool, Nil, ruleResource.ltDefaultRuleIds)

    val addedRulesErrors = ruleResource.rules.groupBy(_.category).toList.flatMap {
      case (_, rules) => {
        val regexRules = rules.collect { case r: RegexRule => r }
        val ltRules = rules.collect { case r: LTRuleXML => r }

        if (regexRules.size > 0) {
          val regexMatcher = new RegexMatcher(regexRules)
          matcherPool.addMatcher(regexMatcher)
        }

        if (ltRules.size > 0) addLTMatcherToPool(matcherPool, ltRules) else Nil
      }
    }

    lastModified = date
    cloudWatchClient.putMetric(Metrics.RulesIngested ,matcherPool.getCurrentRules.size)

    defaultRulesErrors ++ addedRulesErrors match {
      case Nil => Right(())
      case e => Left(e)
    }
  }

  /**
    * Update our matcherPool rules from the S3 bucket.
    */
  def updateRulesFromBucket(): Unit = {
    bucketRuleManager.getRules().map {
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
      case Right(date) if date.compareTo(lastModified) > 0 => updateRulesFromBucket()
      case Right(_) => logger.info("No rule update needed")
      case Left(error) => {
        logger.error("Could not get last modified from S3")
        cloudWatchClient.putMetric(Metrics.RulesNotFound)
      }
    }
  }

  override def run(): Unit = maybeUpdateRulesFromBucket()

  def scheduleUpdateRules(scheduler: Scheduler): Unit = {
    scheduler.scheduleWithFixedDelay(0.seconds, 1.minute)(this)
  }

  private def addLTMatcherToPool(matcherPool: MatcherPool, xmlRules: List[LTRuleXML], defaultRules: List[String] = Nil): List[Throwable]= {
    languageToolFactory.createInstance(xmlRules, defaultRules) match {
      case Right(matcher) => {
        matcherPool.addMatcher(matcher)
        Nil
      }
      case Left(errors) => {
        val logPrefix = "RuleProvisionerService error"
        logger.error(s"${logPrefix}: could not create languageTool instance from ruleResource: ${errors.size} errors found")
        errors.foreach { logger.error(logPrefix, _) }
        errors
      }
    }
  }
}
