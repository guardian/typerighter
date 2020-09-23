package rules

import play.api.libs.json.Json
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest, S3Object}
import java.io.InputStream
import java.util.Date

import com.amazonaws.{AmazonServiceException, SdkClientException}
import play.api.libs.json.Writes
import play.api.libs.json.Reads

import model.{RegexRule, RuleResource}
import play.api.Logging

class BucketRuleManager(s3: AmazonS3, bucketName: String) extends Logging {
    private val RULES_KEY = "rules/typerighter-rules.json"

    def putRules(ruleResource: RuleResource): Either[Exception, Date] = {
        val ruleJson = Json.toJson(ruleResource)
        val bytes = ruleJson.toString.getBytes(java.nio.charset.StandardCharsets.UTF_8.name)

        logOnError("writing rules to S3") {
            val stream: java.io.InputStream = new java.io.ByteArrayInputStream(bytes)
            val metaData = new ObjectMetadata()
            metaData.setContentLength(bytes.length)
            val putObjectRequest = new PutObjectRequest(bucketName, RULES_KEY, stream, metaData)
            val result = s3.putObject(putObjectRequest)
            result.getMetadata.getLastModified
        }
    }

    def getRules(): Either[Exception, (RuleResource, Date)] = {
        logOnError("getting rules from S3") {
            val rules = s3.getObject(bucketName, RULES_KEY)
            val rulesStream = rules.getObjectContent()
            val rulesJson = Json.parse(rulesStream)
            val lastModified = rules.getObjectMetadata.getLastModified
            rules.close()
            (rulesJson.as[RuleResource], lastModified)
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
          logger.info(s"BucketRuleManager: ${name}")
          Right(op)
        } catch {
          case e: Exception => {
                logger.error(s"BucketRuleManager: error whilst $name. ${e.getMessage}")
                Left(e)
            }
        }
    }
}
