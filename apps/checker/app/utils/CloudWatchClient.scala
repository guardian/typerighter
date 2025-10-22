package utils

import software.amazon.awssdk.services.cloudwatch.{CloudWatchClient => AwsCloudWatchClient}
import software.amazon.awssdk.services.cloudwatch.model.Dimension
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit

import com.gu.typerighter.lib.Loggable

object Metrics {
  val RulesIngested = "RulesIngested"
  val RulesNotFound = "RulesNotFound"
  val MatcherPoolJobDurationMs = "MatcherPoolJobDurationMs"
}

class CloudWatchClient(stage: String, dryRun: Boolean) extends Loggable {

  private val cloudWatchClient =
    if (dryRun) None else Some(AwsCloudWatchClient.builder().build())

  def putMetric(metric: String, value: Int = 1): Unit = {

    val dimension = Dimension
      .builder()
      .name("Stage")
      .value(stage.toUpperCase())
      .build()

    val datum = MetricDatum
      .builder()
      .metricName(metric)
      .unit(StandardUnit.COUNT)
      .value(value)
      .dimensions(dimension)
      .build()

    val request = PutMetricDataRequest
      .builder()
      .namespace("Typerighter")
      .metricData(datum)
      .build()

    try {
      cloudWatchClient.map(_.putMetricData(request))
      log.info(
        s"Published $metric metric data with value $value. ${if (dryRun) "DRY RUN" else ""}"
      )
    } catch {
      case e: Exception =>
        log.error(s"CloudWatch putMetricData exception message: ${e.getMessage}", e)
    }
  }
}
