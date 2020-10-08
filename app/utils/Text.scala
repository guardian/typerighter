package utils

import model.{RuleMatch, TextBlock, TextRange}

object Text {
  def getSurroundingText(text: String, from: Int, to: Int, buffer: Int = 100): (String, String) = {
    val precedingText = text.substring(scala.math.max(from - buffer, 0), scala.math.max(from, 0))
    val subsequentText = text.substring(scala.math.min(to, text.length), scala.math.min(to + buffer, text.length))

    (precedingText, subsequentText)
  }

  /**
    * Get a snippet of text as a string to give the context of the matched
    * text at a glance, e.g. "this text has a [[mistaek]]"
    */
  def getMatchTextSnippet(before: String, matchedText: String, after: String) =
    before + "[[" + matchedText + "]]" + after


  /**
    * Remove the given ranges from the block text, adjusting the block range accordingly.
    */
  def removeIgnoredRangesFromBlock(block: TextBlock, ignoredRanges: List[TextRange]): TextBlock = {
    val (newBlock, _) = ignoredRanges.foldRight((block, List.empty[TextRange]))((range, acc) => acc match {
      case (block, rangesAlreadyApplied) => {
        println(rangesAlreadyApplied)
        val mappedRange = rangesAlreadyApplied.foldRight(range)(mapRemovedRange)
        val snipFrom = range.from - block.from
        val snipTo = snipFrom + (range.to - range.from)
        val snipRange = TextRange(Math.max(snipFrom, 0), Math.min(block.to, snipTo) + 1)
        println(block, snipRange)
        val newText = block.text.slice(0, snipRange.from) + block.text.slice(snipRange.to, block.text.size)
        val newBlock = block.copy(text = newText, to = block.to - snipRange.length)
        (newBlock, rangesAlreadyApplied :+ range)
      }
    })

    newBlock
  }

  /**
    * Map a from and to position through the given removed range.
    */
  def mapRemovedRange(incomingRange: TextRange, removedRange: TextRange): TextRange = {
    val charsRemovedBeforeFrom = if (removedRange.from < incomingRange.from) {
      Math.min(incomingRange.from - removedRange.length, removedRange.from - incomingRange.from)
     } else 0
    val charsRemovedBeforeTo = getIntersectionOfRanges(incomingRange, removedRange) match {
      case Some(intersection) => charsRemovedBeforeFrom + intersection.length
      case None => charsRemovedBeforeFrom
    }

    val newFrom = incomingRange.from + charsRemovedBeforeFrom
    val newTo = incomingRange.to + charsRemovedBeforeTo
    // println(incomingRange, removedRange, newFrom, newTo, charsRemovedBeforeTo, charsRemovedBeforeFrom)
    TextRange(newFrom, newTo)
  }

  def getIntersectionOfRanges(rangeA: TextRange, rangeB: TextRange): Option[TextRange] = {
    val range = TextRange(Math.max(rangeA.from, rangeB.from), Math.min(rangeA.to, rangeB.to))
    if (range.length > 0) Some(range) else None
  }
}

// ..[..].........
// .......[..].... – minus four from from and to

// ..[......].....
// .......[..].... – minus six from from, minus (six + three) from two

// ..........[..].
// .......[..].... – minus zero from from, minus one from two

