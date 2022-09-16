package services

import edu.stanford.nlp.ling.CoreLabel

object TokenHelpers {
  // These tokens can contain multiple quotes – we only need to detect the presence of one.
  val NON_WORD_TOKEN_CHARS = Set('`', '\'', '/', '-', '.', ',', ':', ';', '?', '•', '!')
  // These tokens are discrete. LSB == Left Square Bracket, etc.
  val NON_WORD_TOKENS = Set("-LSB-", "-LRB-", "-LCB-", "-RSB-", "-RRB-", "-RCB-")

  def doesNotContainNonWordToken(token: CoreLabel) =
    !TokenHelpers.NON_WORD_TOKENS.contains(token.value)

  def containsWordCharacters(token: CoreLabel) = {
    val tokenWithNonWordCharsRemoved = token.value.filterNot(
      TokenHelpers.NON_WORD_TOKEN_CHARS.contains
    )
    tokenWithNonWordCharsRemoved.size > 0
  }
}
