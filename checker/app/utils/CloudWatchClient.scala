package utils

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;

object Metrics {
  val RulesIngested = "RulesIngested"
  val RulesNotFound = "RulesNotFound"
  val MatcherPoolJobDurationMs = "MatcherPoolJobDurationMs"
}

class CloudWatchClient(stage: String, dryRun: Boolean) extends Loggable {

  private val cloudWatchClient =
    if (dryRun) None else Some(AmazonCloudWatchClientBuilder.defaultClient())

  def putMetric(metric: String, value: Int = 1): Unit = {

    val dimension =
      new Dimension().withName("Stage").withValue(stage.toUpperCase());

    val datum = new MetricDatum()
      .withMetricName(metric)
      .withUnit(StandardUnit.Count)
      .withValue(value)
      .withDimensions(dimension)

    val request = new PutMetricDataRequest()
      .withNamespace("Typerighter")
      .withMetricData(datum)

    try {
      cloudWatchClient.map(_.putMetricData(request))
      log.info(
        s"Published $metric metric data with value ${value}. ${if (dryRun) "DRY RUN"
        else ""}"
      )
    } catch {
      case e: Exception =>
        log.error(
          s"CloudWatch putMetricData exception message: ${e.getMessage}",
          e
        )
    }
  }
}
