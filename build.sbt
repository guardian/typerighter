
name := """philarion"""
organization := "com.gu"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.6"

val languageToolVersion = "4.3"

resolvers += "Spring IO" at "https://repo.spring.io/plugins-release/"

libraryDependencies ++= Seq(
  "org.languagetool" % "languagetool-core" % languageToolVersion,
  "org.languagetool" % "language-en" % languageToolVersion,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
)
