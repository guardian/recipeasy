import com.gu.riffraff.artifact.RiffRaffArtifact.autoImport._
import play.sbt.routes.RoutesKeys._

val CirceVersion = "0.5.0-M2"

val commonSettings = Seq(
  organization := "com.gu",
  description := "recipeasy - structuring recipes",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-target:jvm-1.8", "-Xfatal-warnings"),
  scalacOptions in doc in Compile := Nil,
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-core" % CirceVersion,
    "io.circe" %% "circe-generic" % CirceVersion,
    "io.circe" %% "circe-parser" % CirceVersion,
    "org.postgresql" % "postgresql" % "9.4.1208",
    "io.getquill" %% "quill-jdbc" % "0.9.0",
    "commons-codec" % "commons-codec" % "1.10",
    "com.github.cb372" %% "automagic" % "0.1",
    "com.github.ben-manes.caffeine" % "caffeine" % "2.3.5",
    "com.google.code.findbugs" % "jsr305" % "3.0.1", // required to prevent Caffeine causing compile to fail given -Xfatal-warnings flag.
    "com.amazonaws" % "amazon-kinesis-client" % "1.6.2",
    "org.scalatest" %% "scalatest" % "2.2.6" % Test
  )
)

lazy val flywaySettings = Seq(
  flywayUser := "recipeasy",
  flywayLocations := Seq("filesystem:common/src/main/resources")
  // all other config should be passed in via system properties
  // e.g. sbt -Dflyway.url=jdbc:postgresql://localhost:5432/recipeasy -Dflyway.password=foo
)

lazy val root = (project in file("."))
  .aggregate(ui, common, etl)

def env(key: String): Option[String] = Option(System.getenv(key))

lazy val ui = (project in file("ui"))
  .enablePlugins(PlayScala, RiffRaffArtifact)
  .dependsOn(common)
  .settings(commonSettings)
  .settings(Seq(
    name := "recipeasy",
    libraryDependencies ++= Seq(
      ws,
      "com.gu" %% "play-googleauth" % "0.5.0",
      "com.gu" %% "configuration-magic-play2-4" % "1.2.0",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
      "com.adrianhurt" %% "play-bootstrap" % "1.1-P25-B4",
      "org.quartz-scheduler" % "quartz" % "2.2.3",
      "com.gu" % "kinesis-logback-appender" % "1.2.0",
      "net.logstash.logback" % "logstash-logback-encoder" % "4.6"
    ),
    routesGenerator := InjectedRoutesGenerator,
    riffRaffPackageName := "recipeasy",
    riffRaffPackageType := (packageZipTarball in Universal).value,
    riffRaffManifestProjectName := s"Off-platform::${name.value}",
    riffRaffManifestVcsUrl := "git@github.com:guardian/recipeasy.git",
    riffRaffManifestBranch := env("BRANCH_NAME").getOrElse("unknown_branch"),
    riffRaffBuildIdentifier := env("BUILD_NUMBER").getOrElse("DEV"),
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds")
  ))

lazy val common = (project in file("common"))
  .settings(commonSettings)
  .settings(flywaySettings)
  .settings(Seq(
      libraryDependencies ++= Seq(
          "org.jsoup" % "jsoup" % "1.9.2",
          "com.gu" %% "content-api-client" % "10.17"
      )
  ))

lazy val etl = (project in file("etl"))
  .settings(commonSettings)
  .dependsOn(common)
  .settings(Seq(
      libraryDependencies ++= Seq(
        "org.jsoup" % "jsoup" % "1.9.2",
        "org.typelevel" %% "cats-core" % "0.6.1"
      ),
      cancelable in Global := true
  ))


initialize := {
  val _ = initialize.value
  assert(sys.props("java.specification.version") == "1.8",
    "Java 8 is required for this project.")
}

