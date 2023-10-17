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

class BucketRuleResource(s3: AmazonS3, bucketName: String, stage: String) extends Logging {
  private val RULES_KEY = s"$stage/rules/typerighter-rules-seq.json"
  private val LEGACY_RULES_KEY = s"$stage/rules/typerighter-rules.json"

  def putRules(ruleResource: CheckerRuleResource): Either[Exception, Unit] = {
    val ruleJsonBytes = ArrayBuffer[Byte]();
    ruleResource.rules.foreach(rule => {
      ruleJsonBytes ++= JsonHelpers.toNewlineDeliniatedJson(rule).getBytes()
    })
    logOnError(
      s"writing ${ruleResource.rules.length} rules to S3 at $bucketName/$RULES_KEY"
    ) {
      val stream: java.io.InputStream = new java.io.ByteArrayInputStream(ruleJsonBytes.toArray)
      val metaData = new ObjectMetadata()
      metaData.setContentLength(ruleJsonBytes.length)
      val putObjectRequest = new PutObjectRequest(bucketName, RULES_KEY, stream, metaData)
      val s3Object = s3.putObject(putObjectRequest)
      logger.info(
        s"Artefact created with entity tag: ${s3Object.getMetadata.getETag} - ${ruleJsonBytes.length} bytes written to bucket"
      )
    }
  }

  def getRules(): Either[Exception, (List[CheckerRule], Date)] = {
    val maybeRules =
      try {
        val rules = s3.getObject(bucketName, RULES_KEY)
        val lastModified = rules.getObjectMetadata.getLastModified
        val rulesStream = rules.getObjectContent()
        val rulesArray = ArrayBuffer[CheckerRule]()
        val reader = new BufferedReader(new InputStreamReader(rulesStream))
        reader.lines.forEach(line => {
          rulesArray += Json.parse(line).as[CheckerRule]
        })
        val rulesList = rulesArray.toList
        logger.info(
          s"Got ${rulesList.length} rules from S3 with entity tag: ${rules.getObjectMetadata.getETag}"
        )
        Right((rulesList, lastModified))
      } catch {
        case e: Exception => Left(e)
      }
    maybeRules match {
      case Right((rules, date)) => Right((rules, date))
      case Left(e)              => getLegacyRules(e)
    }
  }

  def getLegacyRules(e: Exception): Either[Exception, (List[CheckerRule], Date)] = {
    logger.info(e.getMessage)
    logger.info("Failed to retrieve and process rules. Looking for legacy artefact.")
    logOnError(s"getting legacy rules from S3 at $bucketName/$LEGACY_RULES_KEY") {
      val rules = s3.getObject(bucketName, LEGACY_RULES_KEY)
      val rulesStream = rules.getObjectContent()
      val rulesJson = Json.parse(rulesStream)
      val lastModified = rules.getObjectMetadata.getLastModified
      rules.close()
      val rulesList = rulesJson.as[CheckerRuleResource].rules
      logger.info(
        s"Got ${rulesList.length} legacy rules from S3 with entity tag: ${rules.getObjectMetadata.getETag}"
      )
      (rulesList, lastModified)
    }
  }

  def getRulesLastModified: Either[Exception, Date] = {
    logOnError("getting the lastModified date from S3") {
      val lastModified =
        try {
          val rulesMeta = s3.getObjectMetadata(bucketName, RULES_KEY)
          rulesMeta.getLastModified
        } catch {
          case _: Throwable =>
            logger.info("Failed to find new artefact, trying legacy artefact")
            val rulesMeta = s3.getObjectMetadata(bucketName, LEGACY_RULES_KEY)
            rulesMeta.getLastModified
        }
      lastModified
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
