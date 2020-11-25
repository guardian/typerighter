import com.gu.riffraff.artifact.BuildInfo

name := """typerighter"""
organization in ThisBuild := "com.gu"
scalaVersion in ThisBuild := "2.12.10"
version in ThisBuild := "1.0-SNAPSHOT"
scalacOptions in ThisBuild := Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps")

val languageToolVersion = "4.3"
val awsSdkVersion = "1.11.571"
val capiModelsVersion = "15.8"
val capiClientVersion = "16.0"
val circeVersion = "0.12.3"

val commonSettings = Seq(
  javaOptions in Universal ++= Seq(
    s"-Dpidfile.path=/dev/null",
    "-J-XX:MaxRAMFraction=2",
    "-J-XX:InitialRAMFraction=2",
    "-J-XX:MaxMetaspaceSize=300m",
    "-J-XX:+PrintGCDetails",
    "-J-XX:+PrintGCDateStamps",
    s"-J-Dlogs.home=/var/log/${packageName.value}",
    s"-J-Xloggc:/var/log/${packageName.value}/gc.log",
    s"-Dconfig.file=/etc/gu/${packageName.value}.conf"
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
    "net.logstash.logback" % "logstash-logback-encoder" % "6.0"
  )
)

val checker = (project in file("checker")).enablePlugins(PlayScala, GatlingPlugin, BuildInfoPlugin, JDebPackaging, SystemdPlugin).settings(
  packageName := "typerighter-checker",
  PlayKeys.devSettings += "play.server.http.port" -> "9100",
  commonSettings,
  resolvers += "Guardian Platform Bintray" at "https://dl.bintray.com/guardian/platforms",
  // Used to resolve xgboost-predictor, which is no longer available
  // at spring.io without auth.
  resolvers += "komiya-atsushi Bintray" at "https://dl.bintray.com/komiya-atsushi/maven",
  libraryDependencies ++= Seq(
    ws,
    "com.gu" %% "simple-configuration-ssm" % "1.5.0",
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
    "com.gu" % "kinesis-logback-appender" % "1.4.4",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
    "org.mockito" %% "mockito-scala-scalatest" % "1.16.2",
    "org.webjars" % "bootstrap" % "4.3.1",
    "com.gu" %% "content-api-models-scala" % capiModelsVersion,
    "com.gu" %% "content-api-models-json" % capiModelsVersion,
    "com.gu" %% "content-api-client-aws" % "0.5",
    "com.gu" %% "content-api-client-default" % capiClientVersion,
    "com.gu" %% "pan-domain-auth-verification" % "0.9.1",
    "com.softwaremill.diffx" %% "diffx-scalatest" % "0.3.29" % Test,
    "biz.k11i" % "xgboost-predictor" % "0.3.1",
    "edu.stanford.nlp" % "stanford-corenlp" % "3.7.0",
    "edu.stanford.nlp" % "stanford-corenlp" % "3.7.0" classifier "models"
  ),
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion),
  libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.0.2" % "test,it",
  libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "3.0.2" % "test,it"
)

val ruleManager = (project in file("rule-manager")).enablePlugins(PlayScala, BuildInfoPlugin, JDebPackaging, SystemdPlugin).settings(
  packageName := "typerighter-rule-manager",
  PlayKeys.devSettings += "play.server.http.port" -> "9101",
  commonSettings,
  libraryDependencies ++= Seq(
    ws,
    guice,
    "net.logstash.logback" % "logstash-logback-encoder" % "6.0"
  )
)

val root = (project in file(".")).aggregate(checker, ruleManager).enablePlugins(RiffRaffArtifact)

riffRaffArtifactResources := Seq(
  (packageBin in Debian in checker).value  -> s"${(packageName in checker).value}/${(packageName in checker).value}.deb",
  (packageBin in Debian in ruleManager).value  -> s"${(packageName in ruleManager).value}/${(packageName in ruleManager).value}.deb",
  baseDirectory.value / "riff-raff.yaml" -> "riff-raff.yaml",
  baseDirectory.value / "typerighter.cfn.yaml" -> "cloudformation/typerighter.cfn.yaml"
)