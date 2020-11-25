package matchers

import model._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffMatcher._

import services.MatcherRequest
import utils.Text
import scala.util.matching.Regex
import scala.concurrent.Future

class NameMatcherTest extends AsyncFlatSpec with Matchers {
  val exampleCategory = Category("example-category", "Example category")

  def getBlocks(text: String) = List(TextBlock("text-block-id", text, 0, text.length))

  val correctArticle = getBlocks("Same as it ever was, heartbreak is bread and butter for the modern multimillion-selling pop star. Where Sam Smith’s last album plumbed the depths of a bad breakup, Love Goes hovers on the brink of the acceptance stage. Opener Young sets an audaciously spare tone, Smith’s voice doubled by vocoder ghosts as they resolve to “get a little wild, get a little high, kiss a hundred boys and not feel like I’m tied to them”. And indeed there’s a refreshing, untethered lightness of mood and touch here, and more interesting music than Smith has lent their nervy vocal flourishes to before. You can hear the influence of Disclosure’s Guy Lawrence (joining Shellback/MXM, Steve Mac and Jimmy Napes on production) on the upbeat Diamonds and the plush and pulsing Dance (Til You Love Someone Else), while Burna Boy makes a great foil on the midnight moodiness of My Oasis. For the Lover That I Lost and Forgive Myself play it a little too safe, with melancholy piano and surface-deep strings, but the Labrinth-collaboration title track saves the day, Smith softly staccato over a looping little baroque piano motif and a subtle, pulsing beat. It’s nice to hear them taking a few small risks. Next, it’d be great to see Smith get really wild.")
  val incorrectArticle = getBlocks("Same as it ever was, heartbreak is bread and butter for the modern multimillion-selling pop star. Where Sam Smith’s last album plumbed the depths of a bad breakup, Love Goes hovers on the brink of the acceptance stage. Opener Young sets an audaciously spare tone, Smith’s voice doubled by vocoder ghosts as he resolves to “get a little wild, get a little high, kiss a hundred boys and not feel like I’m tied to them”. And indeed there’s a refreshing, untethered lightness of mood and touch here, and more interesting music than Smith has lent their nervy vocal flourishes to before. You can hear the influence of Disclosure’s Guy Lawrence (joining Shellback/MXM, Steve Mac and Jimmy Napes on production) on the upbeat Diamonds and the plush and pulsing Dance (Til You Love Someone Else), while Burna Boy makes a great foil on the midnight moodiness of My Oasis. For the Lover That I Lost and Forgive Myself play it a little too safe, with melancholy piano and surface-deep strings, but the Labrinth-collaboration title track saves the day, Smith softly staccato over a looping little baroque piano motif and a subtle, pulsing beat. It’s nice to hear him taking a few small risks. Next, it’d be great to see Smith get really wild.")
  val incorrectSentence = getBlocks("Sam Smith is a singer. He performed a concert last Saturday.")
  val incorrectSentenceSubjectLast = getBlocks("After his first album (In The Lonely Hour), Sam Smith realised the follow up (The Thrill of It All)")
  val allTypesOfPronoun = getBlocks("They took a walk. Sam spoke to them. Their train was late. The ticket wasn't theirs.")

  it should "run a check against a valid article and produce no results" in {
    val nameMatcher = new NameMatcher(List.empty)
    val eventuallyMatches = nameMatcher.check(MatcherRequest(correctArticle))

    eventuallyMatches.map { matches =>
      matches.size shouldBe(0)
    }
  }

  it should "run a check against an invalid block and correctly match against invalid pronouns" in {
    val samSmithRule = NameRule("SAM_SMITH_SINGER", "Sam", "Smith", THEY_THEM, exampleCategory, "Sam Smith is a singer")
    val nameMatcher = new NameMatcher(List(samSmithRule))
//    val eventuallyMatches = nameMatcher.check(MatcherRequest(incorrectArticle))
    val matches1 = nameMatcher.check(MatcherRequest(incorrectSentence))
    val matches2 = nameMatcher.check(MatcherRequest(incorrectSentenceSubjectLast))
    val matches3 = nameMatcher.check(MatcherRequest(allTypesOfPronoun))

    matches2.map { matches =>
      matches.size shouldBe(1)
    }
  }
}
