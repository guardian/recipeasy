import com.gu.riffraff.artifact.RiffRaffArtifact.autoImport._
import play.sbt.routes.RoutesKeys._

val commonSettings = Seq(
  organization := "com.gu",
  description := "recipeasy - structuring recipes",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-target:jvm-1.8", "-Xfatal-warnings"),
  scalacOptions in doc in Compile := Nil
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
      "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
      "org.scalatest" %% "scalatest" % "2.2.6" % "test"
    ),
    routesGenerator := InjectedRoutesGenerator,
    riffRaffPackageName := "recipeasy",
    riffRaffPackageType := (packageZipTarball in Universal).value,
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds")
  ))

lazy val common = (project in file("common"))
  .settings(commonSettings)

initialize := {
  val _ = initialize.value
  assert(sys.props("java.specification.version") == "1.8",
    "Java 8 is required for this project.")
}
