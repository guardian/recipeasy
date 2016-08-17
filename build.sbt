import com.gu.riffraff.artifact.RiffRaffArtifact.autoImport._
import play.sbt.routes.RoutesKeys._

val commonSettings = Seq(
  organization := "com.gu",
  description := "recipeasy - structuring recipes",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-target:jvm-1.8", "-Xfatal-warnings"),
  scalacOptions in doc in Compile := Nil,
  libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % Test

)

lazy val root = (project in file("."))
  .aggregate(ui, common)

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
      "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0"
    ),
    routesGenerator := InjectedRoutesGenerator,
    riffRaffPackageName := "recipeasy",
    riffRaffPackageType := (packageZipTarball in Universal).value,
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds")
  ))

lazy val common = (project in file("common"))
  .settings(commonSettings)

lazy val etl = (project in file("etl"))
  .settings(commonSettings)
  .dependsOn(common)
  .settings(Seq(
      libraryDependencies ++= Seq(
        "com.gu" %% "content-api-client" % "9.4",
        "org.jsoup" % "jsoup" % "1.9.2",
        "org.typelevel" %% "cats-core" % "0.6.1",
        "commons-codec" % "commons-codec" % "1.10"
      ),
      cancelable in Global := true
  ))


initialize := {
  val _ = initialize.value
  assert(sys.props("java.specification.version") == "1.8",
    "Java 8 is required for this project.")
}

