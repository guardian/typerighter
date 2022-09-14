import com.gu.riffraff.artifact.BuildInfo

name := "typerighter"
ThisBuild / organization := "com.gu"
ThisBuild / scalaVersion := "2.13.7"
ThisBuild / version := "1.0-SNAPSHOT"
ThisBuild / scalacOptions := Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps")

val languageToolVersion = "5.1"
val awsSdkVersion = "1.11.999"
val capiModelsVersion = "17.1.1"
val capiClientVersion = "17.23"
val circeVersion = "0.12.3"
val scalikejdbcVersion = scalikejdbc.ScalikejdbcBuildInfo.version
val scalikejdbcPlayVersion = "2.8.0-scalikejdbc-3.5"
val appsFolder = "apps"

val commonSettings = Seq(
  Universal / javaOptions ++= Seq(
    s"-Dpidfile.path=/dev/null",
    "-J-XX:MaxRAMFraction=2",
    "-J-XX:InitialRAMFraction=2",
    "-J-XX:MaxMetaspaceSize=300m",
    "-J-XX:+PrintGCDetails",
    "-J-XX:+PrintGCDateStamps",
    s"-J-Dlogs.home=/var/log/${packageName.value}",
    s"-J-Xloggc:/var/log/${packageName.value}/gc.log"
  ),
  buildInfoPackage := "typerighter",
  buildInfoKeys := {
    lazy val buildInfo = BuildInfo(baseDirectory.value)
    Seq[BuildInfoKey](
      BuildInfoKey.constant("buildNumber", buildInfo.buildIdentifier),
      // so this next one is constant to avoid it always recompiling on dev machines.
      // we only really care about build time on teamcity, when a constant based on when
      // it was loaded is just fine
      BuildInfoKey.constant("buildTime", System.currentTimeMillis),
      BuildInfoKey.constant("gitCommitId", buildInfo.revision)
    )
  },
  libraryDependencies ++= Seq(
    "net.logstash.logback" % "logstash-logback-encoder" % "6.0",
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
    "com.softwaremill.diffx" %% "diffx-scalatest" % "0.3.29" % Test,
    "org.mockito" %% "mockito-scala-scalatest" % "1.16.2",
    "com.gu" % "kinesis-logback-appender" % "1.4.4",
    "com.gu" %% "simple-configuration-ssm" % "1.5.6",
    "com.gu" %% "pan-domain-auth-verification" % "1.0.4",
  ),
  dependencyOverrides ++= Seq(
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.4",
  )
)

val commonLib = (project in file(s"$appsFolder/common-lib"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    packageName := "common-lib",
    commonSettings,
    libraryDependencies ++= Seq(
      // @todo – we're repeating ourselves. Can we derive this from the plugin?
      "com.typesafe.play" %% "play" % "2.8.11",
      "com.gu" % "kinesis-logback-appender" % "1.4.2"
    )
  )

val checker = (project in file(s"$appsFolder/checker"))
  .dependsOn(commonLib)
  .enablePlugins(PlayScala, GatlingPlugin, BuildInfoPlugin, JDebPackaging, SystemdPlugin)
  .settings(
    Universal / javaOptions += s"-Dconfig.file=/etc/gu/${packageName.value}.conf",
    packageName := "typerighter-checker",
    PlayKeys.devSettings += "play.server.http.port" -> "9100",
    commonSettings,
    libraryDependencies ++= Seq(
      ws,
      "org.languagetool" % "languagetool-core" % languageToolVersion,
      "org.languagetool" % "language-en" % languageToolVersion,
      "com.amazonaws" % "aws-java-sdk-ec2" % awsSdkVersion,
      "com.amazonaws" % "aws-java-sdk-s3" % awsSdkVersion,
      "com.amazonaws" % "aws-java-sdk-ssm" % awsSdkVersion,
      "com.amazonaws" % "aws-java-sdk-kinesis" % awsSdkVersion,
      "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsSdkVersion,
      "com.google.api-client" % "google-api-client" % "1.23.0",
      "com.google.oauth-client" % "google-oauth-client-jetty" % "1.23.0",
      "com.google.apis" % "google-api-services-sheets" % "v4-rev516-1.23.0",
      "net.logstash.logback" % "logstash-logback-encoder" % "6.0",
      "org.webjars" % "bootstrap" % "4.3.1",
      "com.gu" %% "content-api-models-scala" % capiModelsVersion,
      "com.gu" %% "content-api-models-json" % capiModelsVersion,
      "com.gu" %% "content-api-client-aws" % "0.7",
      "com.gu" %% "content-api-client-default" % capiClientVersion,
      "edu.stanford.nlp" % "stanford-corenlp" % "3.4",
      "edu.stanford.nlp" % "stanford-corenlp" % "3.4" classifier "models",
      "edu.stanford.nlp" % "stanford-parser" % "3.4",
    ),
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion),
    libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.7.2" % "test,it",
    libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "3.7.2" % "test,it"
  )

val ruleManager = (project in file(s"$appsFolder/rule-manager"))
  .dependsOn(commonLib)
  .enablePlugins(PlayScala, BuildInfoPlugin, JDebPackaging, SystemdPlugin, ScalikejdbcPlugin)
  .settings(
    packageName := "typerighter-rule-manager",
    PlayKeys.devSettings += "play.server.http.port" -> "9101",
    commonSettings,
    libraryDependencies ++= Seq(
      ws,
      guice,
      jdbc,
      evolutions,
      "org.postgresql" % "postgresql" % "42.2.5",
      "org.scalikejdbc" %% "scalikejdbc" % scalikejdbcVersion,
      "org.scalikejdbc" %% "scalikejdbc-config" % scalikejdbcVersion,
      "org.scalikejdbc" %% "scalikejdbc-play-initializer" % scalikejdbcPlayVersion,
      "org.scalikejdbc" %% "scalikejdbc-test" % "3.5.0" % Test,
      "com.gu" %% "pan-domain-auth-play_2-8" % "1.0.6",
      "com.gu" %% "editorial-permissions-client" % "2.14"
    )
  )

val root = (project in file(".")).aggregate(commonLib, checker, ruleManager).enablePlugins(RiffRaffArtifact)

riffRaffArtifactResources := Seq(
  (checker / Debian / packageBin).value  -> s"${(checker / packageName).value}/${(checker / packageName).value}.deb",
  (ruleManager / Debian / packageBin).value  -> s"${(ruleManager / packageName).value}/${(ruleManager / packageName).value}.deb",
  baseDirectory.value / "riff-raff.yaml" -> "riff-raff.yaml",
  baseDirectory.value / "cdk/cdk.out/typerighter.template.json" -> "typerighter-cloudformation/typerighter.template.json"
)

riffRaffManifestProjectName := s"Editorial Tools::Typerighter"
