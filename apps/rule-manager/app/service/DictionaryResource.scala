package service

import com.amazonaws.services.s3.AmazonS3
import com.gu.typerighter.lib.SafeXMLParser
import com.gu.typerighter.model.{TaggedWordlist, WordTag}
import play.api.data.FormError
import play.api.libs.json.Json
import utils.Dictionary
import xs4s.XMLStream
import scala.util.{Failure, Success, Try}

class DictionaryResource(s3: AmazonS3, bucketName: String, stage: String) {
  private val DICTIONARY_KEY = s"$stage/dictionary/collins-dictionary.xml"
  private val LEMMATISED_LIST_KEY = s"$stage/dictionary/collins-lemmatised-list.xml"
  private val WORDS_TO_NOT_PUBLISH_KEY = s"$stage/dictionary/words-to-not-publish.json"

  def getDictionaryWords(): Either[Seq[FormError], Set[String]] = {
    val words = Try({
      val dictionaryInputStream = s3.getObject(bucketName, DICTIONARY_KEY).getObjectContent
      val dictionaryXmlReader = XMLStream.fromInputStream(dictionaryInputStream)
      val wordsFromDictionary = Dictionary.dictionaryXmlToWordList(dictionaryXmlReader)
      dictionaryInputStream.close()

      val lemmatisedListInputStream = s3.getObject(bucketName, LEMMATISED_LIST_KEY).getObjectContent
      val lemmatisedListXml = SafeXMLParser.load(dictionaryInputStream)
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
}
