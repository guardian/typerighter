import play.sbt._
import scala.sys.process._
class ViteBuildHook(label: String, prefix: String) extends PlayRunHook {
  val devnullLogger: ProcessLogger = ProcessLogger((_: String) => {})

  override def afterStarted(): Unit = {

    val emoji = prefix match {
        case "manager" => "🚀"
        case "checker" => "⭐"
        case _ => "🔥"
    }

    println(
      s"$emoji $label started and available at https://$prefix.typerighter.local.dev-gutools.co.uk"
    )
  }
}