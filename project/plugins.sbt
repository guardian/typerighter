addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.20")

addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "1.1.8")

addSbtPlugin("io.gatling" % "gatling-sbt" % "3.0.0")

libraryDependencies += "org.vafer" % "jdeb" % "1.7" artifacts Artifact("jdeb", "jar", "jar")