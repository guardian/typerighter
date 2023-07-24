import com.gu.riffraff.artifact.BuildInfo
import sys.process._

name := "typerighter"
ThisBuild / organization := "com.gu"
ThisBuild / scalaVersion := "2.13.7"
ThisBuild / version := "1.0-SNAPSHOT"
ThisBuild / scalacOptions := Seq(
  "-encoding",
  "UTF-8",
  "-target:jvm-1.8",
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

val languageToolVersion = "6.0"
val awsSdkVersion = "1.12.416"
val capiModelsVersion = "17.5.1"
val capiClientVersion = "19.2.1"
val circeVersion = "0.14.1"
val scalikejdbcVersion = scalikejdbc.ScalikejdbcBuildInfo.version
val scalikejdbcPlayVersion = "2.8.0-scalikejdbc-3.5"
val appsFolder = "apps"

def javaVersionNumber = {
  IO.read(new File(".java-version"))
}

def removeStartingOne(javaVersionString: String): String = {
  val startsWithOne = "^1\\.".r
  startsWithOne.replaceAllIn(javaVersionString, "")
}

def parseJavaVersionNumber(javaVersionString: String): String = {
  removeStartingOne(javaVersionString).split('.').head
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
  libraryDependencies ++= Seq(
    "com.amazonaws" % "aws-java-sdk-secretsmanager" % awsSdkVersion,
    "net.logstash.logback" % "logstash-logback-encoder" % "7.2",
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
    "com.softwaremill.diffx" %% "diffx-scalatest-should" % "0.8.2" % Test,
    "org.mockito" %% "mockito-scala-scalatest" % "1.17.12",
    "com.gu" %% "simple-configuration-ssm" % "1.5.7",
    "com.gu" %% "pan-domain-auth-play_2-8" % "1.2.1",
    "com.google.api-client" % "google-api-client" % "2.0.1",
    "com.google.apis" % "google-api-services-sheets" % "v4-rev20221216-2.0.0",
    "org.languagetool" % "languagetool-core" % languageToolVersion,
    "org.languagetool" % "language-en" % languageToolVersion,
    "com.gu" %% "content-api-models-scala" % capiModelsVersion,
    "com.gu" %% "content-api-models-json" % capiModelsVersion,
    "com.gu" %% "content-api-client-aws" % "0.7",
    "com.gu" %% "content-api-client-default" % capiClientVersion,
    "com.gu" %% "panda-hmac-play_2-8" % "2.2.0"
  ),
  dependencyOverrides ++= Seq(
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.4",
  )
)

val commonLib = (project in file(s"$appsFolder/common-lib"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      ws,
      // @todo – we're repeating ourselves. Can we derive this from the plugin?
      "com.typesafe.play" %% "play" % "2.8.19",
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
        "-J-XX:+PrintGCDateStamps",
        s"-J-Dlogs.home=/var/log/${packageName.value}",
        s"-J-Xloggc:/var/log/${packageName.value}/gc.log"
      ),
      commonSettings,
      initialize := {
        val _ = initialize.value
        val expectedJavaVersion = parseJavaVersionNumber(javaVersionNumber)
        val actualJavaVersion = removeStartingOne(sys.props("java.specification.version"))
        assert(
          expectedJavaVersion.equals(actualJavaVersion),
          s"Java $expectedJavaVersion is required for this project. You are using Java $actualJavaVersion."
        )
      },
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
      "io.gatling"            % "gatling-test-framework"    % "3.7.2" % "test,it"
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
      "org.postgresql" % "postgresql" % "42.5.1",
      "org.scalikejdbc" %% "scalikejdbc" % scalikejdbcVersion,
      "org.scalikejdbc" %% "scalikejdbc-config" % scalikejdbcVersion,
      "org.scalikejdbc" %% "scalikejdbc-play-initializer" % scalikejdbcPlayVersion,
      "org.scalikejdbc" %% "scalikejdbc-test" % scalikejdbcVersion % Test,
      "org.scalikejdbc" %% "scalikejdbc-syntax-support-macro" % scalikejdbcVersion,
      "com.gu" %% "editorial-permissions-client" % "2.14",
    )
  )

val root = (project in file(".")).aggregate(commonLib, checker, ruleManager).enablePlugins(RiffRaffArtifact)

riffRaffArtifactResources := Seq(
  (checker / Debian / packageBin).value  -> s"${(checker / packageName).value}/${(checker / packageName).value}.deb",
  (ruleManager / Debian / packageBin).value  -> s"${(ruleManager / packageName).value}/${(ruleManager / packageName).value}.deb",
  baseDirectory.value / "riff-raff.yaml" -> "riff-raff.yaml",
  baseDirectory.value / "cdk/cdk.out/typerighter-CODE.template.json" -> "typerighter-cloudformation/typerighter-CODE.template.json",
  baseDirectory.value / "cdk/cdk.out/typerighter-PROD.template.json" -> "typerighter-cloudformation/typerighter-PROD.template.json"
)

riffRaffManifestProjectName := s"Editorial Tools::Typerighter"
