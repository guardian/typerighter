addDependencyTreePlugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.9.4")
addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "1.1.18")
addSbtPlugin("io.gatling" % "gatling-sbt" % "3.0.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0") // "2.4.0" is just sbt plugin version
addSbtPlugin("org.scalikejdbc" %% "scalikejdbc-mapper-generator" % "3.5.0")

libraryDependencies ++= Seq("org.vafer" % "jdeb" % "1.7" artifacts Artifact("jdeb", "jar", "jar"), "org.postgresql" % "postgresql" % "42.5.1")
