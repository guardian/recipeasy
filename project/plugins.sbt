addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.4")
addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "0.9.5")

resolvers += Resolver.typesafeRepo("releases")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

resolvers += "Flyway" at "https://flywaydb.org/repo"
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.0.3")
