package rules

import play.api.libs.json.Json
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest, S3Object}
import java.io.InputStream

import com.amazonaws.{AmazonServiceException, SdkClientException}
import model.RegexRule
import utils.Loggable

import scala.io.Source.fromInputStream


class BucketRuleResource (s3: AmazonS3, rulesKey: String) extends Loggable {
    private val BUCKET_NAME = "typerighter-rules"

    def serialiseAndUploadRules(rules: List[RegexRule]) {
        val ruleJson = Json.toJson(rules)
        val stream: java.io.InputStream = new java.io.ByteArrayInputStream(ruleJson.toString.getBytes(java.nio.charset.StandardCharsets.UTF_8.name))
        uploadRules(stream)
    }

    def uploadRules(stream: InputStream) {
        try {
            val metaData = new ObjectMetadata()
            val putObjectRequest = new PutObjectRequest(BUCKET_NAME, rulesKey, stream, metaData)
            val result = s3.putObject(putObjectRequest)
        }
        catch  {
            case e: AmazonServiceException => {
                log.warn(e.getMessage)
            }
            case e: SdkClientException => {
                log.warn(e.getMessage)
            }
        }
    }

    def getRules(): Option[List[RegexRule]] = {
        try {
            val rules = s3.getObject(BUCKET_NAME, rulesKey)
            val rulesStream = rules.getObjectContent()
            val rulesJson = Json.parse(rulesStream)
            rules.close()
            Some(rulesJson.as[List[RegexRule]])
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
