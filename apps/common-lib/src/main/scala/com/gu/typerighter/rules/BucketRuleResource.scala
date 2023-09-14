package com.gu.typerighter.rules

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.gu.typerighter.lib.SafeXMLParser
import com.gu.typerighter.model.{CheckerRuleResource, TaggedWordlist, WordTag}
import play.api.Logging
import play.api.data.FormError
import play.api.libs.json.Json

import java.util.Date
import scala.util.{Failure, Success, Try}

class BucketRuleResource(s3: AmazonS3, bucketName: String, stage: String) extends Logging {
  private val RULES_KEY = s"$stage/rules/typerighter-rules.json"
  private val DICTIONARY_KEY = s"$stage/dictionary/typerighter-dictionary.xml"
  private val WORDS_TO_NOT_PUBLISH_KEY = s"$stage/dictionary/words-to-not-publish.json"

  def putRules(ruleResource: CheckerRuleResource): Either[Exception, Unit] = {
    val ruleJson = Json.toJson(ruleResource)
    val bytes = Json.toBytes(ruleJson)

    logOnError(
      s"writing rules to S3 at $bucketName/$RULES_KEY with JSON hash ${ruleJson.hashCode}"
    ) {
      val stream: java.io.InputStream = new java.io.ByteArrayInputStream(bytes)
      val metaData = new ObjectMetadata()
      metaData.setContentLength(bytes.length)
      val putObjectRequest = new PutObjectRequest(bucketName, RULES_KEY, stream, metaData)
      s3.putObject(putObjectRequest)
    }
  }

  def getRules(): Either[Exception, (CheckerRuleResource, Date)] = {
    logOnError(s"getting rules from S3 at $bucketName/$RULES_KEY") {
      val rules = s3.getObject(bucketName, RULES_KEY)
      val rulesStream = rules.getObjectContent()
      val rulesJson = Json.parse(rulesStream)
      val lastModified = rules.getObjectMetadata.getLastModified
      rules.close()

      logger.info(s"Got rules from S3. JSON hash: ${rulesJson.hashCode()}")
      (rulesJson.as[CheckerRuleResource], lastModified)
    }
  }

  def getDictionaryWords(): Either[Seq[FormError], List[String]] = {
    val words = Try({
      val dictionary = s3.getObject(bucketName, DICTIONARY_KEY).getObjectContent()
      val dictionaryXml = SafeXMLParser.load(dictionary)
      val words = Dictionary.dictionaryXmlToWordList(dictionaryXml)
      dictionary.close()
      words
    })

    words match {
      case Success(words) => Right(words)
      case Failure(exception) =>
        Left(Seq(FormError("dictionary-parse-error", exception.getMessage)))
    }
  }

  def getWordsToNotPublish(): List[WordTag] = {
    val wordlistsToNotPublish = Try({
      val words = s3.getObject(bucketName, WORDS_TO_NOT_PUBLISH_KEY)
      val wordsStream = words.getObjectContent()
      val wordlistsToNotPublishJson = Json.parse(wordsStream)
      words.close()
      wordlistsToNotPublishJson.as[List[TaggedWordlist]]
    })

    wordlistsToNotPublish match {
      case Success(wordsToNotPublish) =>
        wordsToNotPublish.flatMap(wordlist =>
          wordlist.words.map(word => WordTag(word, wordlist.tag))
        )
      case Failure(_) =>
        Nil
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
