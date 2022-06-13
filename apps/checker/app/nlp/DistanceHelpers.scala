package nlp

import info.debatty.java.stringsimilarity.JaroWinkler

object DistanceHelpers {
  /**
   * We use Jaro-Winkler to calculate distance for names, as there's some
   * evidence it's especially well suited to that task. See e.g.
   * http://users.cecs.anu.edu.au/~Peter.Christen/publications/tr-cs-06-02.pdf
   */
  lazy val jaroWinkler = new JaroWinkler

  def findSimilarNames(candidateName: String, names: Set[String], distanceThreshold: Double = 0.2): List[String] = {
    val namesAndScores = names.foldLeft(Set.empty[(Double, String)]) {
      case (acc, name) => {
        jaroWinkler.distance(candidateName, name) match {
          case distance if distance < distanceThreshold =>
            acc + ((distance, name))
          case _ => acc
        }
      }
    }

    namesAndScores
      .toList
      .sortBy { case (score, _) => score }
      .map { case (_, name) => name }
  }
}
