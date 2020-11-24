package simulation

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import play.api.libs.json.Json
import java.util.UUID
import model.Check
import model.TextBlock

class CheckPermutationsSimulation extends Simulation {
  val httpConf = http
    .baseUrl("http://localhost:9100")
    .contentTypeHeader("application/json")

  val request = http("checkRequest")
    .post("/check")
    .body(StringBody(_ => {
      val articleText = getRandomArticle
      val check = Check(
        Some("documentId"),
        UUID.randomUUID.toString(),
        None,
        List(TextBlock("blockId", articleText, 0, articleText.size))
      )
      Json.toJson(check).toString
    }))

  val scn = scenario("CheckPermutationsSimulation")
    .exec(request)

  val populatedScenario = scn.inject(
    atOnceUsers(2)
  )

  setUp(populatedScenario).protocols(httpConf)

  val articleFragments = List("Michel Barnier has warned that the move led by Labour MP Yvette Cooper to block the prime minister from delivering a no-deal Brexit is doomed to fail unless a majority for an alternative agreement is found",
    "The EU’s chief negotiator, in a speech in Brussels, said the “default” for the UK was still crashing out if MPs could not coalesce around a new vision of its future outside the bloc",
    "“There appears to be a majority in the Commons to oppose a no-deal but opposing a no-deal will not stop a no-deal from happening at the end of March”, he said. “To stop ‘no deal’, a positive majority for another solution will need to emerge.” Labour appears set to whip its MPs to back Cooper’s amendment paving the way for legislation that would mandate ministers to extend article 50 if a no-deal Brexit looked imminent",
    "Advertisement Barnier said that extending the two years of the negotiating period beyond 29 March should not be the primary focus for the UK parliament",
    "“We need decisions more than we need time actually”, he said. “I don’t know whether postponing or extending will be raised but its the head of state and government that will have to answer that question by consensus. Some have said to me that if the question is raised, then why would we do that? What would the purpose be? How long would be required?” There have been growing fears in Brussels that the UK is heading for a crash landing out of the bloc",
    " Sign up to our Brexit weekly briefing  Read more EU ambassadors on Wednesday were urged by the European commission to ensure that their national contingency measures did not replicate the terms of today in order to keep the pressure on parliament to forge a “positive majority” for a deal",
    "Barnier reiterated in his address to the European economic and social committee, a civil society organisation, that he believed the key to getting an agreement through parliament lay in the prime minister, Theresa May, embracing a permanent customs union as backed by Labour",
    "Senior EU diplomats are concerned, however, that this push by Brussels, which would involve redrafting the political declaration on the future relationship accompanying the withdrawal agreement, is falling on deaf ears",
    "“If the UK red lines were to evolve in the next few weeks or months the union would be ready immediately and open to other models of relationships which are more ambitious”, Barnier said. “We’re ready to rework the content and ambition of the political declaration.” It is understood that Downing Street’s chief Brexit adviser, Olly Robbins, has been told by EU officials that there was little point in the prime minister returning to Brussels to seek concessions on the Irish backstop",
    "In an interview with Le Monde, Rzeczpospolita and Luxemburger Wort published earlier on Wednesday, Barnier made public his belief that May’s strategy of trying to secure a time-limit was doomed to fail",
    "In comments that appear to put a wrecking ball to the prime minister’s strategy, he said the withdrawal agreement in all its facets was the “the only possible option” for Britain as it leaves",
    "“The question of limiting the backstop in time has already been discussed twice by European leaders”, Barnier said. “This is the only possible option because an insurance is of no use if it is time limited",
    "“We cannot tie the backstop to a time limit”, he added. “Imagine if your home’s insurance was limited to five years and you’d have a problem after six years … That’s difficult to justify. It’s similar with the backstop.” Under the backstop, the UK would stay in a customs union with the EU unless an alternative arrangement could avoid the imposition of a hard border on the island of Ireland")

  def getRandomArticle = {
    util.Random.shuffle(articleFragments).mkString
  }
}

