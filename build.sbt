
organization := "com.gu"
description := "recipeasy - structuring recipes"
scalaVersion := "2.11.8"
name := "recipeasy"
scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-target:jvm-1.8", "-Xfatal-warnings")
scalacOptions in doc in Compile := Nil

lazy val recipeasy = (project in file(".")).enablePlugins(PlayScala, RiffRaffArtifact)

resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= Seq(
  ws,
  "com.gu" %% "play-googleauth" % "0.5.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

routesGenerator := InjectedRoutesGenerator
riffRaffPackageName := "recipeasy"
riffRaffPackageType := (packageZipTarball in Universal).value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")


initialize := {
  val _ = initialize.value
  assert(sys.props("java.specification.version") == "1.8",
    "Java 8 is required for this project.")
}
