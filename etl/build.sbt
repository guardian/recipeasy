scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.gu" %% "content-api-client" % "9.4",
  "org.jsoup" % "jsoup" % "1.9.2",
  "org.typelevel" %% "cats-core" % "0.6.1",
  "org.scalatest" %% "scalatest" % "2.2.6" % Test
)

cancelable in Global := true
