package com.gu.typerighter.rules

import scala.xml.{Elem, Node}

object Dictionary {
  def lemmaOrInflListToText(node: Node): List[String] = {
    node match {
      case node if node.label == "lemma" => List(node.text)
      case node if node.label == "infl_list" =>
        node.child.filter(_.label == "infl").map(_.text).toList
      case _ => Nil
    }
  }

  def dictionaryXmlToWordList(dictionaryXml: Elem): List[String] = {
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
    words.distinct.filterNot(_.forall(_.isWhitespace))
  }
}
