package com.gu.typerighter.rules

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{
  PutObjectRequest,
  GetObjectRequest,
  GetObjectAttributesRequest
}
import software.amazon.awssdk.core.sync.RequestBody
import com.gu.typerighter.lib.JsonHelpers
import com.gu.typerighter.model.{CheckerRule, CheckerRuleResource}
import play.api.Logging
import play.api.libs.json.Json

import java.util.Date
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

class BucketRuleResource(s3: S3Client, bucketName: String, stage: String) extends Logging {
  private val RULES_KEY = s"$stage/rules/typerighter-rules-seq.json"
  private val LEGACY_RULES_KEY = s"$stage/rules/typerighter-rules.json"

  def putRules(ruleResource: CheckerRuleResource): Either[Exception, Unit] = {
    val ruleJsonBytes = ArrayBuffer[Byte]();
    ruleResource.rules.foreach(rule => {
      ruleJsonBytes ++= JsonHelpers.toNewlineDelineatedJson(rule).getBytes()
    })
    logOnError(
      s"writing ${ruleResource.rules.length} rules to S3 at $bucketName/$RULES_KEY"
    ) {
      val stream: java.io.InputStream = new java.io.ByteArrayInputStream(ruleJsonBytes.toArray)
      val putObjectRequest = PutObjectRequest
        .builder()
        .bucket(bucketName)
        .key(RULES_KEY)
        .contentLength(ruleJsonBytes.length)
        .build()
      val s3Object =
        s3.putObject(putObjectRequest, RequestBody.fromInputStream(stream, ruleJsonBytes.length))
      logger.info(
        s"Artefact created with entity tag: ${s3Object.eTag} - ${ruleJsonBytes.length} bytes written to bucket"
      )
    }
  }

  def getRules(): Either[Exception, (List[CheckerRule], Date)] = {
    val maybeRules: Either[Exception, (List[CheckerRule], Date)] =
      try {
        val rulesStream = s3.getObject(
          GetObjectRequest
            .builder()
            .bucket(bucketName)
            .key(RULES_KEY)
            .build()
        )
        val lastModified = rulesStream.response().lastModified
        val rulesList = Source
          .fromInputStream(rulesStream)
          .getLines()
          .map(line => {
            Json.parse(line).as[CheckerRule]
          })
          .toList
        logger.info(
          s"Got ${rulesList.length} rules from S3 with entity tag: ${rulesStream.response().eTag}"
        )
        rulesStream.close()
        Right((rulesList, Date.from(lastModified)))
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
      val rulesStream =
        s3.getObject(GetObjectRequest.builder().bucket(bucketName).key(LEGACY_RULES_KEY).build())
      val rulesJson = Json.parse(rulesStream)
      val lastModified = rulesStream.response().lastModified
      rulesStream.close()
      val rulesList = rulesJson.as[CheckerRuleResource].rules
      logger.info(
        s"Got ${rulesList.length} legacy rules from S3 with entity tag: ${rulesStream.response().eTag}"
      )
      (rulesList, Date.from(lastModified))
    }
  }

  def getRulesLastModified: Either[Exception, Date] = {
    logOnError("getting the lastModified date from S3") {
      val lastModified =
        try {
          val rulesMeta = s3.getObjectAttributes(
            GetObjectAttributesRequest
              .builder()
              .bucket(bucketName)
              .key(RULES_KEY)
              .build()
          )
          Date.from(rulesMeta.lastModified())
        } catch {
          case _: Throwable =>
            logger.info("Failed to find new artefact, trying legacy artefact")
            val rulesMeta = s3.getObjectAttributes(
              GetObjectAttributesRequest
                .builder()
                .bucket(bucketName)
                .key(LEGACY_RULES_KEY)
                .build()
            )
            Date.from(rulesMeta.lastModified())
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
