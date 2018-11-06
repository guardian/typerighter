name := """philarion"""
organization := "com.gu"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, RiffRaffArtifact, JDebPackaging, SystemdPlugin)

riffRaffArtifactResources := Seq(
  (packageBin in Debian).value -> s"${name.value}/${name.value}.deb",
  baseDirectory.value / "riff-raff.yaml" -> "riff-raff.yaml",
  baseDirectory.value / "philarion.cfn.yaml" -> "cloudformation/philarion.cfn.yaml"
)

javaOptions in Universal ++= Seq(
  s"-Dpidfile.path=/dev/null",
  "-J-XX:MaxRAMFraction=2",
  "-J-XX:InitialRAMFraction=2",
  "-J-XX:MaxMetaspaceSize=300m",
  "-J-XX:+PrintGCDetails",
  "-J-XX:+PrintGCDateStamps",
  s"-J-Dlogs.home=/var/log/${packageName.value}",
  s"-J-Xloggc:/var/log/${packageName.value}/gc.log",
  "-Dconfig.file=/etc/gu/philarion.conf"
)

scalaVersion := "2.12.6"

val languageToolVersion = "4.3"

resolvers += "Spring IO" at "https://repo.spring.io/plugins-release/"

libraryDependencies ++= Seq(
  "org.languagetool" % "languagetool-core" % languageToolVersion,
  "org.languagetool" % "language-en" % languageToolVersion,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
)
