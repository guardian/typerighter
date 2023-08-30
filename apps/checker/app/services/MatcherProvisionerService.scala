package services

import java.util.Date
import akka.actor.Scheduler
import com.gu.typerighter.model.{
  CheckerRule,
  CheckerRuleResource,
  DictionaryRule,
  LTRuleCore,
  LTRuleXML,
  RegexRule
}
import com.gu.typerighter.rules.BucketRuleResource
import matchers.{DictionaryMatcher, LanguageToolFactory, RegexMatcher}
import play.api.Logging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import utils.{CloudWatchClient, Matcher, Metrics}

class MatcherProvisionerService(
    bucketRuleResource: BucketRuleResource,
    matcherPool: MatcherPool,
    languageToolFactory: LanguageToolFactory,
    cloudWatchClient: CloudWatchClient
)(implicit ec: ExecutionContext)
    extends Logging
    with Runnable {

  var lastModified: Date = new Date(0)

  /** Update the rules in our matcherPool, given a ruleResource.
    */
  def updateRules(ruleResource: CheckerRuleResource, date: Date): Either[List[Throwable], Unit] = {
    matcherPool.removeAllMatchers()

    val coreRules = ruleResource.rules.collect { case r: LTRuleCore => r }
    val coreRulesErrors = addLTMatcherToPool(matcherPool, Nil, coreRules)

    val addedRulesErrors =
      ruleResource.rules.groupBy(_.category).toList.flatMap { case (_, rules) =>
        val regexRules = rules.collect { case r: RegexRule => r }
        val ltRules = rules.collect { case r: LTRuleXML => r }

        if (regexRules.nonEmpty) {
          val regexMatcher = new RegexMatcher(regexRules)
          matcherPool.addMatcher(regexMatcher)
        }

        val dictionaryRules = rules.collect { case r: DictionaryRule => r }
        if (dictionaryRules.nonEmpty) {
          println(dictionaryRules.head)
          matcherPool.addMatcher(new DictionaryMatcher(dictionaryRules))
        }
        if (ltRules.nonEmpty) addLTMatcherToPool(matcherPool, ltRules) else Nil
      }

    lastModified = date
    cloudWatchClient.putMetric(Metrics.RulesIngested, matcherPool.getCurrentRules.size)

    coreRulesErrors ++ addedRulesErrors match {
      case Nil => Right(())
      case e   => Left(e)
    }
  }

  /** Update our matcherPool rules from the S3 bucket.
    */
  def updateRulesFromBucket(): Unit = {
    bucketRuleResource.getRules().map { case (ruleResource, date) =>
      updateRules(ruleResource, date)
    }
  }

  /** Update our matcherPool rules from the S3 bucket, if what's in the bucket is newer that what's
    * in memory.
    */
  def maybeUpdateRulesFromBucket(): Unit = {
    bucketRuleResource.getRulesLastModified match {
      case Right(date) if date.compareTo(lastModified) > 0 => updateRulesFromBucket()
      case Right(_)                                        => logger.info("No rule update needed")
      case Left(error) =>
        logger.error(s"Could not get last modified from S3", error)
        cloudWatchClient.putMetric(Metrics.RulesNotFound)
    }
  }

  override def run(): Unit = maybeUpdateRulesFromBucket()

  def scheduleUpdateRules(scheduler: Scheduler): Unit = {
    scheduler.scheduleWithFixedDelay(0.seconds, 5.seconds)(this)
  }

  private def addLTMatcherToPool(
      matcherPool: MatcherPool,
      xmlRules: List[LTRuleXML],
      coreRules: List[LTRuleCore] = List.empty
  ): List[Throwable] = {
    languageToolFactory.createInstance(xmlRules, coreRules.map(_.languageToolRuleId)) match {
      case Right(matcher) =>
        matcherPool.addMatcher(matcher)
        Nil
      case Left(errors) =>
        val logPrefix = "RuleProvisionerService error"
        logger.error(
          s"$logPrefix: could not create languageTool instance from ruleResource: ${errors.size} errors found"
        )
        errors.foreach { logger.error(logPrefix, _) }
        errors
    }
  }

  def getMatcherForRule(rule: CheckerRule): Either[List[Throwable], Matcher] = {
    rule match {
      case rule: LTRuleCore =>
        languageToolFactory.createInstance(List.empty, List(rule.languageToolRuleId))
      case rule: LTRuleXML => languageToolFactory.createInstance(List(rule), List.empty)
      case rule: RegexRule => Right(new RegexMatcher(List(rule)))
      case rule            => Left(List(new Error(s"Cannot get matcher for rule type $rule")))
    }
  }
}
