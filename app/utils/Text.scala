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
    val (newBlock, _) = ignoredRanges.foldLeft((block, List.empty[TextRange]))((acc, range) => acc match {
      case (block, rangesAlreadyApplied) => {
        val mappedRange = rangesAlreadyApplied.foldRight(range)(mapRemovedRange)
        val snipFrom = mappedRange.from - block.from
        val snipTo = snipFrom + (mappedRange.to - mappedRange.from)
        val snipRange = TextRange(Math.max(snipFrom, 0), Math.min(block.to, snipTo) + 1)

        val newText = block.text.slice(0, snipRange.from) + block.text.slice(snipRange.to, block.text.size)
        val newBlock = block.copy(text = newText, to = block.from + newText.length)

        (newBlock, rangesAlreadyApplied :+ mappedRange)
      }
    })

    newBlock
  }

    /**
    * Map the range this match applies to through the given ranges, adjusting its range accordingly.
    */
  def mapMatchThroughIgnoredRanges(ruleMatch: RuleMatch, ignoredRanges: List[TextRange]): RuleMatch = {
    val (newMatch, _) = ignoredRanges.foldLeft((ruleMatch, List.empty[TextRange]))((acc, range) => acc match {
      case (ruleMatch, rangesAlreadyApplied) => {
        val mappedRange = rangesAlreadyApplied.foldRight(range)(mapAddedRange)
        val newMatchRange = mapAddedRange(mappedRange, TextRange(ruleMatch.fromPos, ruleMatch.toPos))
        val newRuleMatch = ruleMatch.copy(fromPos = newMatchRange.from, toPos = newMatchRange.to)
        (newRuleMatch, rangesAlreadyApplied :+ mappedRange)
      }
    })

    newMatch
  }

  /**
    * Map a from and to position through the given removed range.
    */
  def mapRemovedRange(removedRange: TextRange, incomingRange: TextRange): TextRange = {
    val charsRemovedBeforeFrom = if (removedRange.from < incomingRange.from) {
      val rangeBetweenRemovedStartAndIncomingStart = TextRange(removedRange.from, incomingRange.from)
      getIntersectionOfRanges(removedRange, rangeBetweenRemovedStartAndIncomingStart).map(_.length).getOrElse(0)
    } else 0

    val charsRemovedBeforeTo = getIntersectionOfRanges(incomingRange, removedRange) match {
      case Some(intersection) => charsRemovedBeforeFrom + intersection.length
      case None => charsRemovedBeforeFrom
    }

    val newFrom = incomingRange.from - charsRemovedBeforeFrom
    val newTo = incomingRange.to - charsRemovedBeforeTo

    TextRange(newFrom, newTo)
  }

    /**
    * Map a from and to position through the given added range.
    */
  def mapAddedRange(addedRange: TextRange, incomingRange: TextRange): TextRange = {
    val charsAddedBeforeFrom = if (addedRange.from <= incomingRange.from) {
      val rangeBetweenAddedStartAndIncomingStart = TextRange(addedRange.from, incomingRange.to)
      getIntersectionOfRanges(addedRange, rangeBetweenAddedStartAndIncomingStart).map(_.length).getOrElse(0)
    } else 0

    val charsAddedBeforeTo = getIntersectionOfRanges(incomingRange, addedRange) match {
      case Some(intersection) => addedRange.length
      case None => charsAddedBeforeFrom
    }

    val newFrom = incomingRange.from + charsAddedBeforeFrom
    val newTo = incomingRange.to + charsAddedBeforeTo

    TextRange(newFrom, newTo)
  }

  def getIntersectionOfRanges(rangeA: TextRange, rangeB: TextRange): Option[TextRange] = {
    val range = TextRange(Math.max(rangeA.from, rangeB.from), Math.min(rangeA.to, rangeB.to))
    if (range.length > 0) Some(range) else None
  }
}

