package utils

object Text {
  def getSurroundingText(text: String, from: Int, to: Int, buffer: Int = 100): (String, String) = {
    val textBefore = text.substring(scala.math.max(from - buffer, 0), scala.math.max(from, 0))
    val textAfter = text.substring(scala.math.min(to, text.length), scala.math.min(to + buffer, text.length))

    (textBefore, textAfter)
  }

  /**
    * Get a snippet of text as a string to give the context of the matched
    * text at a glance, e.g. "this text has a [[mistaek]]"
    */
  def getMatchTextSnippet(before: String, matchedText: String, after: String) =
    before + "[[" + matchedText + "]]" + after
}
