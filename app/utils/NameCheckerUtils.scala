package utils

import info.debatty.java.stringsimilarity.JaroWinkler

object NameCheckerUtils {
  val distanceFinder = new JaroWinkler

  def findSimilarLastNames(nameStr: String, otherNames: List[String]): List[String] = {
    val fullNames = otherNames.filter(_.split(" ").length > 1)
    val namesAndScores = fullNames.foldLeft(Set.empty[(Double, String)]) {
      case (acc, otherName) => {
        val lastName = otherName.split(" ").last
        println(s"Matching name $nameStr against lastname $lastName")
        distanceFinder.distance(nameStr, lastName) match {
          case distance if distance < 0.3 =>
            acc + ((distance, otherName))
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
