addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.19")
addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "1.1.18")
addSbtPlugin("io.gatling" % "gatling-sbt" % "3.2.1")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0") // "2.4.0" is just sbt plugin version
addSbtPlugin("org.scalikejdbc" %% "scalikejdbc-mapper-generator" % "3.5.0")

libraryDependencies ++= Seq(
  "org.vafer" % "jdeb" % "1.7" artifacts Artifact("jdeb", "jar", "jar"),
  "org.postgresql" % "postgresql" % "42.5.4"
)
