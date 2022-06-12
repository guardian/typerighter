package services

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffMatcher._

class TokenizerTest extends AsyncFlatSpec with Matchers {

  val tokenizer = new Tokenizer()

  behavior of "tokenize"

  it should "tokenize words" in {
    val sentence = "An example sentence"
    val tokens = tokenizer.tokenize(sentence)

    tokens should matchTo(List(
      ("An", 0, 2),
      ("example", 3, 10),
      ("sentence", 11, 19)
    ))
  }

  it should "ignore non-word characters" in {
    val sentence = "An [example] -- sentence, too."
    val tokens = tokenizer.tokenize(sentence)

    tokens should matchTo(List(
      ("An", 0, 2),
      ("example", 4, 11),
      ("sentence", 16, 24),
      ("too", 26, 29)
    ))
  }

  it should "concatenate contractions into single words" in {
    val sentence = "Shouldn't haven't can't Lis's"
    val tokens = tokenizer.tokenize(sentence)

    tokens should matchTo(List(
      ("Shouldn't", 0, 9),
      ("haven't",10,17),
      ("can't",18,23),
      ("Lis's",24,29)
    ))
  }
}
