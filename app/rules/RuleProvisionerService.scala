package rules

import java.util.Date

import akka.actor.Scheduler
import matchers.RegexMatcher
import services.MatcherPool
import utils.Loggable

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class RuleProvisionerService(ruleBucket: BucketRuleResource, matcherPool: MatcherPool)(implicit ec: ExecutionContext) extends Loggable with Runnable {
  var lastModified: Date = new Date(0)

  def updateRules(): Unit = {
    ruleBucket.getRules match {
      case Some((rules, date)) => {
        matcherPool.removeAllMatchers()
        rules.groupBy(_.category).foreach { case (category, rules) => {
          val matcher = new RegexMatcher(category.name, rules)
          matcherPool.addMatcher(category, matcher)
          lastModified = date
        }}
      }
      case _ => log.error(s"Could not get rules from S3")
    }
  }

  def maybeUpdateRules(): Unit = {
    ruleBucket.getRulesLastModified match {
      case Some(date) if date.compareTo(lastModified) > 0 => updateRules
      case Some(_) => log.info("No rule update needed")
      case None => log.error("Could not get last modified from S3")
    }
  }

  override def run(): Unit = maybeUpdateRules

  def scheduleUpdateRules(scheduler: Scheduler): Unit = {
    scheduler.scheduleWithFixedDelay(0.seconds, 1.minute)(this)
  }

}
