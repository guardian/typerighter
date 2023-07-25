package com.gu.typerighter.lib

import java.io.InputStream
import javax.xml.parsers.SAXParserFactory
import scala.xml.{Elem, XML}
object SafeXMLParser {
  def load(is: InputStream): Elem = {
    val xmlParserFactory = SAXParserFactory.newInstance()
    xmlParserFactory.setNamespaceAware(false)
    xmlParserFactory.setFeature(
      "http://apache.org/xml/features/nonvalidating/load-external-dtd",
      false
    )
    val xmlParser = xmlParserFactory.newSAXParser()
    XML.withSAXParser(xmlParser).load(is)
  }
}
