import com.gu.riffraff.artifact.BuildInfo

name := """typerighter"""
organization := "com.gu"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, RiffRaffArtifact, JDebPackaging, SystemdPlugin,
  GatlingPlugin, BuildInfoPlugin)

riffRaffArtifactResources := Seq(
  (packageBin in Debian).value -> s"${name.value}/${name.value}.deb",
  baseDirectory.value / "riff-raff.yaml" -> "riff-raff.yaml",
  baseDirectory.value / "typerighter.cfn.yaml" -> "cloudformation/typerighter.cfn.yaml"
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
  "-Dconfig.file=/etc/gu/typerighter.conf"
)

buildInfoPackage := "typerighter"

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
}

resolvers += "Spring IO" at "https://repo.spring.io/plugins-release/"
resolvers += "Guardian Platform Bintray" at "https://dl.bintray.com/guardian/platforms"

val languageToolVersion = "4.3"
val awsSdkVersion = "1.11.571"

libraryDependencies ++= Seq(
  "com.gu" %% "simple-configuration-ssm" % "1.5.0",
  "org.languagetool" % "languagetool-core" % languageToolVersion,
  "org.languagetool" % "language-en" % languageToolVersion,
  "com.amazonaws" % "aws-java-sdk-ec2" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-ssm" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-kinesis" % awsSdkVersion,
  "com.google.api-client" % "google-api-client" % "1.23.0",
  "com.google.oauth-client" % "google-oauth-client-jetty" % "1.23.0",
  "com.google.apis" % "google-api-services-sheets" % "v4-rev516-1.23.0",
  "net.logstash.logback" % "logstash-logback-encoder" % "6.0",
  "com.gu" % "kinesis-logback-appender" % "1.4.4",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  "org.webjars" % "bootstrap" % "4.3.1",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.4",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.4" classifier "models",
  "edu.stanford.nlp" % "stanford-parser" % "3.4",
  "info.debatty" % "java-string-similarity" % "0.10",
  ws
)

scalaVersion := "2.12.8"
scalacOptions := Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps")
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.0.2" % "test,it"
libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "3.0.2" % "test,it"