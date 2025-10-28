package service

import software.amazon.awssdk.services.s3.S3Client
import com.gu.typerighter.lib.SafeXMLParser
import com.gu.typerighter.model.{TaggedWordlist, WordTag}
import play.api.data.FormError
import play.api.libs.json.Json
import utils.Dictionary
import xs4s.XMLStream
import scala.util.{Failure, Success, Try}
import software.amazon.awssdk.services.s3.model.GetObjectRequest

class DictionaryResource(s3: S3Client, bucketName: String, stage: String) {
  private val DICTIONARY_KEY = s"$stage/dictionary/collins-dictionary.xml"
  private val LEMMATISED_LIST_KEY = s"$stage/dictionary/collins-lemmatised-list.xml"
  private val WORDS_TO_NOT_PUBLISH_KEY = s"$stage/dictionary/words-to-not-publish.json"

  def getDictionaryWords(): Either[Seq[FormError], Set[String]] = {
    val words = Try({
      val dictionaryInputStream = s3.getObject(
        GetObjectRequest
          .builder()
          .bucket(bucketName)
          .key(DICTIONARY_KEY)
          .build()
      )
      val dictionaryXmlReader = XMLStream.fromInputStream(dictionaryInputStream)
      val wordsFromDictionary = Dictionary.dictionaryXmlToWordList(dictionaryXmlReader)
      dictionaryInputStream.close()

      val lemmatisedListInputStream = s3.getObject(
        GetObjectRequest
          .builder()
          .bucket(bucketName)
          .key(LEMMATISED_LIST_KEY)
          .build()
      )
      val lemmatisedListXml = SafeXMLParser.load(lemmatisedListInputStream)
      val wordsFromLemmatisedList = Dictionary.lemmatisedListXmlToWordList(lemmatisedListXml)
      lemmatisedListInputStream.close()

      wordsFromDictionary ++ wordsFromLemmatisedList
    })

    words match {
      case Success(words) => Right(words)
      case Failure(exception) =>
        Left(Seq(FormError("dictionary-parse-error", exception.getMessage)))
    }
  }

  def getWordsToNotPublish(): List[WordTag] = {
    val wordlistsToNotPublish = Try({
      val wordsStream = s3.getObject(
        GetObjectRequest
          .builder()
          .bucket(bucketName)
          .key(WORDS_TO_NOT_PUBLISH_KEY)
          .build()
      )
      val wordlistsToNotPublishJson = Json.parse(wordsStream)
      wordsStream.close()
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
}
