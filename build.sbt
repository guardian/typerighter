import com.gu.riffraff.artifact.BuildInfo
import sys.process._

name := "typerighter"
ThisBuild / organization := "com.gu"
ThisBuild / scalaVersion := "2.13.11"
ThisBuild / version := "1.0-SNAPSHOT"
ThisBuild / scalacOptions := Seq(
  "-encoding",
  "UTF-8",
  "-release:11",
  "-deprecation",
  "-Xfatal-warnings",
  "-Xlint:unused",
  "-feature",
  "-unchecked",
  "-language:implicitConversions",
  "-language:postfixOps",
  // https://github.com/playframework/twirl/issues/105
  "-Wconf:src=twirl/.*:s"
)

// Prevent the output of dependencyTree being truncated to avoid misreporting dependencies.
// See https://support.snyk.io/hc/en-us/articles/9590215676189-Deeply-nested-Scala-projects-have-dependencies-truncated
ThisBuild / asciiGraphWidth := 999999999

val languageToolVersion = "6.0"
val awsSdkVersion = "1.12.576"
val capiModelsVersion = "17.5.1"
val capiClientVersion = "19.2.1"
val pandaVersion = "3.0.1"
val circeVersion = "0.14.1"
val scalikejdbcVersion = scalikejdbc.ScalikejdbcBuildInfo.version
val scalikejdbcPlayVersion = "2.8.0-scalikejdbc-3.5"
val appsFolder = "apps"

def javaVersionNumber = {
  IO.read(new File(".java-version"))
}

val jackson = {
  val version = "2.14.2"
  Seq(
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % version,
    "com.fasterxml.jackson.core" % "jackson-core" % version,
    "com.fasterxml.jackson.core" % "jackson-databind" % version,
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % version
  )
}

val commonSettings = Seq(
  Test / fork := false, // Enables attaching debugger in tests
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
  //Necessary to override jackson versions due to AWS and Play incompatibility
  dependencyOverrides ++= jackson,
  //Necessary to override json to resolve vulnerabilities introduced by languagetool-core
  dependencyOverrides ++= Seq("org.json" % "json" % "20231013"),
  libraryDependencies ++= Seq(
    "com.amazonaws" % "aws-java-sdk-secretsmanager" % awsSdkVersion,
    "net.logstash.logback" % "logstash-logback-encoder" % "7.2",
    "org.scalatestplus.play" %% "scalatestplus-play" % "6.0.1" % Test,
    "com.softwaremill.diffx" %% "diffx-scalatest-should" % "0.8.2" % Test,
    "org.mockito" %% "mockito-scala-scalatest" % "1.17.30",
    "com.gu" %% "simple-configuration-ssm" % "1.6.4",
    "com.gu" %% "pan-domain-auth-play_2-9" % pandaVersion,
    "com.google.api-client" % "google-api-client" % "2.0.1",
    "com.google.apis" % "google-api-services-sheets" % "v4-rev20221216-2.0.0",
    "org.languagetool" % "languagetool-core" % languageToolVersion,
    "org.languagetool" % "language-en" % languageToolVersion,
    "com.gu" %% "content-api-models-scala" % capiModelsVersion,
    "com.gu" %% "content-api-models-json" % capiModelsVersion,
    "com.gu" %% "content-api-client-aws" % "0.7",
    "com.gu" %% "content-api-client-default" % capiClientVersion,
    "com.gu" %% "panda-hmac-play_2-9" % pandaVersion,
    "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.29",
    "com.scalawilliam" %% "xs4s-core" % "0.9.1",
    "ch.qos.logback" % "logback-classic" % "1.4.4", // manually overwriting logback-classic to resolve issue in Play framework: https://github.com/playframework/playframework/issues/11499
),
  libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

val commonLib = (project in file(s"$appsFolder/common-lib"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      ws,
      // @todo – we're repeating ourselves. Can we derive this from the plugin?
      "com.typesafe.play" %% "play" % "2.9.4",
    )
  )

def playProject(label: String, projectName: String, domainPrefix: String, devHttpPorts: Map[String, String]) =
  Project(projectName, file(s"$appsFolder/$projectName"))
    .dependsOn(commonLib)
    .enablePlugins(PlayScala, BuildInfoPlugin, JDebPackaging, SystemdPlugin)
    .settings(
      PlayKeys.devSettings ++= devHttpPorts.map { case (protocol, value) => s"play.server.$protocol.port" -> value }.toSeq,
      PlayKeys.playRunHooks += new ViteBuildHook(label, domainPrefix),
      Universal / javaOptions ++= Seq(
        s"-Dpidfile.path=/dev/null",
        "-J-XX:MaxRAMFraction=2",
        "-J-XX:InitialRAMFraction=2",
        "-J-XX:MaxMetaspaceSize=300m",
        "-J-XX:+PrintGCDetails",
        s"-J-Dlogs.home=/var/log/${packageName.value}",
        s"-J-Xloggc:/var/log/${packageName.value}/gc.log"
      ),
      commonSettings,
    )

val checker = playProject(
  "Rule checker",
  "checker",
  "checker",
  Map("http" -> "9100")
)
  .enablePlugins(GatlingPlugin)
  .settings(
    Universal / javaOptions += s"-Dconfig.file=/etc/gu/${packageName.value}.conf",
    packageName := "typerighter-checker",
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk-ec2" % awsSdkVersion,
      "com.amazonaws" % "aws-java-sdk-s3" % awsSdkVersion,
      "com.amazonaws" % "aws-java-sdk-ssm" % awsSdkVersion,
      "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsSdkVersion,
      "net.logstash.logback" % "logstash-logback-encoder" % "6.0",
      "org.webjars" % "bootstrap" % "4.3.1",
      "com.gu" %% "content-api-models-scala" % capiModelsVersion,
      "com.gu" %% "content-api-models-json" % capiModelsVersion,
      "com.gu" %% "content-api-client-aws" % "0.7",
      "com.gu" %% "content-api-client-default" % capiClientVersion,
      "org.apache.opennlp" % "opennlp" % "2.1.0",
      "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.7.2" % "test,it",
      "io.gatling"            % "gatling-test-framework"    % "3.7.2" % "test,it",
      "org.carrot2" % "morfologik-tools" % "2.1.7"
    ) ++ Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion)
  )

val ruleManager = playProject(
  "Rule manager",
  "rule-manager",
  "manager",
  Map("http" -> "9101")
)
  .enablePlugins(ScalikejdbcPlugin)
  .settings(
    packageName := "typerighter-rule-manager",
    libraryDependencies ++= Seq(
      guice,
      jdbc,
      evolutions,
      "org.postgresql" % "postgresql" % "42.5.5",
      "org.scalikejdbc" %% "scalikejdbc" % scalikejdbcVersion,
      "org.scalikejdbc" %% "scalikejdbc-config" % scalikejdbcVersion,
      "org.scalikejdbc" %% "scalikejdbc-play-initializer" % scalikejdbcPlayVersion,
      "org.scalikejdbc" %% "scalikejdbc-test" % scalikejdbcVersion % Test,
      "org.scalikejdbc" %% "scalikejdbc-syntax-support-macro" % scalikejdbcVersion,
      "com.gu" %% "editorial-permissions-client" % "2.14",
    ),
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
  )

val root = (project in file(".")).aggregate(commonLib, checker, ruleManager)
