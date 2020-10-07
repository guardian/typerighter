package utils

object Text {
  def getSurroundingText(text: String, from: Int, to: Int, buffer: Int = 100): String = {

    val textBefore = text.substring(scala.math.max(from - buffer, 0), scala.math.max(from, 0))
    val textMatch = text.substring(from, to)
    val textAfter = text.substring(scala.math.min(to, text.length), scala.math.min(to + buffer, text.length))

    textBefore + "[[" + textMatch + "]]" + textAfter
  }
}
