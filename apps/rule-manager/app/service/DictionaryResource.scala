package com.gu.typerighter.rules

import com.amazonaws.services.s3.AmazonS3
import com.gu.typerighter.lib.SafeXMLParser
import com.gu.typerighter.model.{TaggedWordlist, WordTag}
import play.api.data.FormError
import play.api.libs.json.Json

import javax.xml.stream.XMLEventReader
import scala.xml.{Elem, Node}
import xs4s.XMLStream

import xs4s.syntax.core._
import xs4s.syntax.generic._
import xs4s.XmlElementExtractor

import scala.util.{Failure, Success, Try}

case class CollinsEntry(
                         headword: String,
                         definition: Seq[Definition],
                         pos: Option[String],
                         inflections: Set[String] = Set.empty
                       )

case class Definition(
                       text: String,
                       underlyingSubjects: Seq[String],
                       generalSubjects: Seq[String]
                     )

class DictionaryResource(s3: AmazonS3, bucketName: String, stage: String) {
  private val DICTIONARY_KEY = s"$stage/dictionary/collins-dictionary.xml"
  private val LEMMATISED_LIST_KEY = s"$stage/dictionary/collins-lemmatised-list.xml"
  private val WORDS_TO_NOT_PUBLISH_KEY = s"$stage/dictionary/words-to-not-publish.json"

  def getDictionaryWords(): Either[Seq[FormError], Set[String]] = {
    val words = Try({
      val dictionaryInputStream = s3.getObject(bucketName, DICTIONARY_KEY).getObjectContent
      val dictionaryXmlReader = XMLStream.fromInputStream(dictionaryInputStream)
      val wordsFromDictionary = dictionaryXmlToWordList(dictionaryXmlReader)
      dictionaryInputStream.close()

      val lemmatisedListInputStream = s3.getObject(bucketName, LEMMATISED_LIST_KEY).getObjectContent
      val lemmatisedListXml = SafeXMLParser.load(dictionaryInputStream)
      val wordsFromLemmatisedList = lemmatisedListXmlToWordList(lemmatisedListXml)
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

  def lemmaOrInflListToText(node: Node): List[String] = {
    node match {
      case node if node.label == "lemma" => List(node.text)
      case node if node.label == "infl_list" =>
        node.child.filter(_.label == "infl").map(_.text).toList
      case _ => Nil
    }
  }

  def lemmatisedListXmlToWordList(dictionaryXml: Elem): Set[String] = {
    // The following relies on the XML document having quite a specific structure:
    // - one top-level "lemma_list" node, containing:
    //   - n child "entry" nodes, each containing:
    //     - one "lemma" node with a single string as its text
    //     - zero or more "infl_list" nodes, each containing:
    //       - one or more "infl" nodes with a single string as its text
    val entries = dictionaryXml.child.toList
    val words = for {
      entry <- entries
      lemmaOrInfl <- entry.child
      lemmaOrInflList <- lemmaOrInfl.toList
      word <- lemmaOrInflListToText(lemmaOrInflList)
    } yield word
    words.toSet.filterNot(_.forall(_.isWhitespace))
  }

  def dictionaryXmlToWordList(xmlReader: XMLEventReader): Set[String] = {
    val entries = getDictionaryEntriesFromXml(xmlReader)

    entries.foldLeft(Set.empty[String])((acc, entry) =>
      (acc + entry.headword) ++ entry.inflections
    )
  }

  val IPAStressMarkUpper = "ˈ"
  val IPAStressMarkLower = "ˌ"

  def extractSubjects(senseNode: Node, tagName: String) =
    (senseNode \ tagName).flatMap { subject =>
      (subject \ "@value").toString.split(":").toList
    }

  private def getDictionaryEntriesFromXml(xmlReader: XMLEventReader) = {
    val entryExtractor = XmlElementExtractor.filterElementsByName("superentry")
    val iterator = for {
      superentry <- xmlReader.toIterator.through(
        entryExtractor.scannerThrowingOnError
      )
      entry <- superentry \ "entry"
      headwordNode <- (entry \\ "hwblk" \ "hwgrp" \ "hwunit" \ "hw")
      word <- maybeGetEntryFromXML(headwordNode.text, entry)
    } yield word

    iterator.toSet
  }

  private def maybeGetEntryFromXML(
      rawHeadword: String,
      entry: Node
  ): Option[CollinsEntry] = {
    if (!shouldIncludeEntry(entry)) None
    else {
      val headword = cleanXmlStr(rawHeadword)

      val inflections = (entry \\ "datablk" \ "gramcat" \ "inflgrp" \ "influnit" \ "infl")
        .filter(node => (node \ "@type").text != "partial")
        .map(node => cleanXmlStr(node.text))
        .toSet

      val senses = entry \\ "datablk" \ "gramcat" \ "sensecat"

      val definitions = senses.map { sense =>
        val definition = (sense \ "defgrp" \ "defunit" \ "def").headOption
          .map(_.child.map(_.toString).mkString(""))
          .getOrElse("")
          .replaceAll("[\n\r\t\f]", "")
          .replaceAll("  ", "")

        val underlyingSubjects = extractSubjects(sense, "ulsubjfld")
        val generalSubjects = extractSubjects(sense, "lbsubjfld")

        Definition(definition, underlyingSubjects, generalSubjects)
      }

      val maybePOS = (entry \\ "pospunit" \ "posp").headOption
        .flatMap(_.attribute("value").flatMap(_.headOption.map(_.text)))

      Some(CollinsEntry(headword, definitions, maybePOS, inflections))
    }
  }

  private def shouldIncludeEntry(entry: Node) = {
    // Do not include prefixes
    val partsOfSpeech =
      entry \ "datablk" \ "gramcat" \ "pospgrp" \ "pospunit" \ "posp"
    if ((partsOfSpeech \ "@value").text != "prefix") true
    else false
  }

  private def cleanXmlStr(str: String) = str
    .replaceAll(s"[${IPAStressMarkUpper}${IPAStressMarkLower}]", "")
    .trim
}
