package utils

import model.RuleMatch

object RuleMatchHelpers {
  def removeOverlappingRules(currentMatches: List[RuleMatch], incomingMatches: List[RuleMatch]): List[RuleMatch] =
    currentMatches.filter { currentMatch =>
      incomingMatches.forall { incomingMatch =>
        currentMatch.fromPos < incomingMatch.fromPos && currentMatch.toPos < incomingMatch.fromPos ||
          currentMatch.fromPos > incomingMatch.toPos
      }
    }
}
