package rules

import play.api.libs.json.Json
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest, S3Object}
import java.io.InputStream
import java.util.Date

import com.amazonaws.{AmazonServiceException, SdkClientException}
import model.RegexRule
import utils.Loggable

class BucketRuleResource (s3: AmazonS3, bucketName: String) extends Loggable {
    private val RULES_KEY = "rules/typerighter-rules.json"

    def serialiseAndUploadRules(rules: List[RegexRule]): Either[String, Date] = {
        val ruleJson = Json.toJson(rules)
        uploadRules(ruleJson.toString.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
    }

    def uploadRules(bytes: Array[Byte]): Either[String, Date] = {
        try {
            val stream: java.io.InputStream = new java.io.ByteArrayInputStream(bytes)
            val metaData = new ObjectMetadata()
            metaData.setContentLength(bytes.length)
            val putObjectRequest = new PutObjectRequest(bucketName, RULES_KEY, stream, metaData)
            val result = s3.putObject(putObjectRequest)
            Right(result.getMetadata.getLastModified)
        }
        catch  {
            case e: AmazonServiceException => {
                log.warn(e.getMessage)
                Left(e.getMessage)
            }
            case e: SdkClientException => {
                log.warn(e.getMessage)
                Left(e.getMessage)
            }
        }
    }

    def getRules(): Option[(List[RegexRule], Date)] = {
        try {
            val rules = s3.getObject(bucketName, RULES_KEY)
            val rulesStream = rules.getObjectContent()
            val rulesJson = Json.parse(rulesStream)
            val lastModified = rules.getObjectMetadata.getLastModified
            rules.close()
            Some((rulesJson.as[List[RegexRule]], lastModified))
        }
        catch  {
            case e: AmazonServiceException => {
                log.warn(e.getMessage)
                None
            }
            case e: SdkClientException => {
                log.warn(e.getMessage)
                None
            }
        }
    }

    def getRulesLastModified: Option[Date] = {
        try {
            val rulesMeta = s3.getObjectMetadata(bucketName, RULES_KEY)
            Some(rulesMeta.getLastModified)
        }
        catch  {
            case e: AmazonServiceException => {
                log.warn(e.getMessage)
                None
            }
            case e: SdkClientException => {
                log.warn(e.getMessage)
                None
            }
        }
    }
  
}
