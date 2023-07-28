package com.gu.typerighter.lib

import java.io.InputStream
import javax.xml.parsers.SAXParserFactory
import scala.xml.{Elem, XML}

object SafeXMLParser {
  val xmlParserFactory = SAXParserFactory.newInstance()
  xmlParserFactory.setNamespaceAware(false)
  xmlParserFactory.setFeature(
    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
    false
  )
  val xmlParser = xmlParserFactory.newSAXParser()

  def load(is: InputStream): Elem = {
    XML.withSAXParser(xmlParser).load(is)
  }
}
