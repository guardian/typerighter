package model

import play.api.libs.json.{Json, Reads}

object TextRange {
  implicit val reads = Json.reads[TextRange]
}

case class TextRange(from: Int, to: Int) {
  def length = to - from

  /**
    * Map a from and to position through the given removed range.
    */
  def mapRemovedRange(removedRange: TextRange): TextRange = {
    val charsRemovedBeforeFrom = if (removedRange.from < this.from) {
      val rangeBetweenRemovedStartAndIncomingStart = TextRange(removedRange.from, this.from)
      rangeBetweenRemovedStartAndIncomingStart.getIntersection(removedRange).map(_.length).getOrElse(0)
    } else 0

    val charsRemovedBeforeTo = this.getIntersection(removedRange) match {
      case Some(intersection) => charsRemovedBeforeFrom + intersection.length
      case None => charsRemovedBeforeFrom
    }

    val newFrom = this.from - charsRemovedBeforeFrom
    val newTo = this.to - charsRemovedBeforeTo

    TextRange(newFrom, newTo)
  }

    /**
    * Map a from and to position through the given added range.
    */
  def mapAddedRange(addedRange: TextRange): TextRange = {
    val charsAddedBeforeFrom = if (addedRange.from <= this.from) {
      val rangeBetweenAddedStartAndIncomingStart = TextRange(addedRange.from, this.to)
      rangeBetweenAddedStartAndIncomingStart.getIntersection(addedRange).map(_.length).getOrElse(0)
    } else 0

    val charsAddedBeforeTo = this.getIntersection(addedRange) match {
      case Some(intersection) => addedRange.length
      case None => charsAddedBeforeFrom
    }

    val newFrom = this.from + charsAddedBeforeFrom
    val newTo = this.to + charsAddedBeforeTo

    TextRange(newFrom, newTo)
  }

  def getIntersection(rangeB: TextRange): Option[TextRange] = {
    val range = TextRange(Math.max(this.from, rangeB.from), Math.min(this.to, rangeB.to))
    if (range.length > 0) Some(range) else None
  }
}
