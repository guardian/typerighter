package utils

import xs4s.syntax.core._
import xs4s.syntax.generic._
import xs4s.XmlElementExtractor

import javax.xml.stream.XMLEventReader
import scala.xml.{Elem, Node}

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

object Dictionary {

  def lemmaOrInflListToText(node: Node): Set[String] = {
    node match {
      case node if node.label == "lemma" => Set(node.text)
      case node if node.label == "infl_list" =>
        node.child.filter(_.label == "infl").map(_.text).toSet
      case _ => Set.empty
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

  /** Get a word list from a Collins dictionary file.
    *
    * We must stream the XML, rather than read it in one large chunk, due to its size.
    */
  def dictionaryXmlToWordList(xmlReader: XMLEventReader): Set[String] = {
    val entries = getDictionaryEntriesFromXml(xmlReader)

    entries.foldLeft(Set.empty[String])((acc, entry) => (acc + entry.headword) ++ entry.inflections)
  }

  val IPAStressMarkUpper = "ˈ"
  val IPAStressMarkLower = "ˌ"

  def extractSubjects(senseNode: Node, tagName: String) =
    (senseNode \ tagName).flatMap { subject =>
      (subject \ "@value").toString.split(":").toList
    }

  def getDictionaryEntriesFromXml(xmlReader: XMLEventReader) = {
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
