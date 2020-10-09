package model

import play.api.libs.json.{Json, Reads, Writes}

/**
  * A block of text to match against.
  */
case class TextBlock(id: String, text: String, from: Int, to: Int) {
  /**
    * Remove the given ranges from the block text, adjusting the block range accordingly.
    */
  def removeIgnoredRanges(ignoredRanges: List[TextRange]): TextBlock = {
    val (newBlock, _) = ignoredRanges.foldLeft((this, List.empty[TextRange]))((acc, range) => acc match {
      case (block, rangesAlreadyApplied) => {
        val mappedRange = rangesAlreadyApplied.foldRight(range)((acc, range) => range.mapRemovedRange(acc))
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
}

object TextBlock {
  implicit val reads: Reads[TextBlock] = Json.reads[TextBlock]
  implicit val writes: Writes[TextBlock] = Json.writes[TextBlock]
}
