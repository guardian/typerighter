package com.gu.typerighter.rules

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.gu.typerighter.lib.JsonHelpers
import com.gu.typerighter.model.{CheckerRule, CheckerRuleResource}
import play.api.Logging
import play.api.libs.json.{Json}

import java.io.{BufferedReader, InputStreamReader}
import java.util.Date
import scala.collection.mutable.ArrayBuffer

class BucketRuleResource(s3: AmazonS3, bucketName: String, stage: String) extends Logging  {
  private val RULES_KEY = s"$stage/rules/typerighter-rules-seq.json"

  def putRules(ruleResource: CheckerRuleResource): Either[Exception, Unit] = {
    val ruleJsonBytes = ArrayBuffer[Byte]();
    ruleResource.rules.foreach(rule => {
      ruleJsonBytes ++= JsonHelpers.toNewlineDeliniatedJson(rule).getBytes()
    })
    logOnError(
    logOnError(
      s"writing rules to S3 at $bucketName/$RULES_KEY with JSON hash ${ruleJsonBytes.hashCode}"
    ) {
      val stream: java.io.InputStream = new java.io.ByteArrayInputStream(ruleJsonBytes.toArray)
      val metaData = new ObjectMetadata()
      metaData.setContentLength(ruleJsonBytes.length)
      val putObjectRequest = new PutObjectRequest(bucketName, RULES_KEY, stream, metaData)
      s3.putObject(putObjectRequest)
      logger.info(s"${ruleJsonBytes.length} bytes written to bucket")
    }
  }

  def getRules(): Either[Exception, (List[CheckerRule], Date)] = {
      val rules = s3.getObject(bucketName, RULES_KEY)
      val lastModified = rules.getObjectMetadata.getLastModified
      val rulesStream = rules.getObjectContent()
      val rulesArray = ArrayBuffer[CheckerRule]()
      val reader = new BufferedReader(new InputStreamReader(rulesStream))
      val error = try {
        reader.lines.forEach(line => {
          rulesArray += Json.parse(line).as[CheckerRule]
        })
        None
      } catch {
        case e: Exception =>
          logger.error(s"BucketRuleManager: error whilst reading rules - ${e.getMessage}", e)
          Some(e)
      }

      logger.info(s"Got rules from S3. JSON hash: ${rules.hashCode()}")
      error match {
        case None => Right((rulesArray.toList, lastModified))
        case Some(error) => Left(error)
      }
  }

  def getRulesLastModified: Either[Exception, Date] = {
    logOnError("getting the lastModified date from S3") {
      val rulesMeta = s3.getObjectMetadata(bucketName, RULES_KEY)
      rulesMeta.getLastModified
    }
  }

  def logOnError[T](name: String)(op: => T): Either[Exception, T] = {
    try {
      logger.info(s"BucketRuleManager: $name")
      Right(op)
    } catch {
      case e: Exception =>
        logger.error(s"BucketRuleManager: error whilst $name. ${e.getMessage}", e)
        Left(e)
    }
  }
}
