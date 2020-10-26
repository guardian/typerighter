package utils

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;

object Metrics {
  val RulesIngested = "RulesIngested"
}

class CloudWatchClient(stage: String) extends Loggable {

  private val builder = AmazonCloudWatchClientBuilder.defaultClient()

  private lazy val cloudWatchClient = builder

  def putRulesIngested(numberOfRules: Int): Unit = {

    val dimension = new Dimension().withName("Stage").withValue(stage.toUpperCase());

    val datum = new MetricDatum()
      .withMetricName(Metrics.RulesIngested)
      .withUnit(StandardUnit.Count)
      .withValue(numberOfRules)
      .withDimensions(dimension)

    val request = new PutMetricDataRequest().withNamespace("Typerighter").withMetricData(datum)

    try {
      val result = cloudWatchClient.putMetricData(request)
      log.info(s"Published metric data: $result")
    } catch {
      case e: Exception =>
        log.error(s"CloudWatch putMetricData exception message: ${e.getMessage}", e)
    }
  }
}
