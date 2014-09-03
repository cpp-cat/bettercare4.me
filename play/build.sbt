name := """bettercare4.me"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.4" % "test",
  "org.apache.spark"  %% "spark-core"    % "1.1.0-SNAPSHOT",
  "com.github.tototoshi" %% "scala-csv" % "1.0.0",
  "joda-time" % "joda-time" % "2.4",
  "org.yaml" % "snakeyaml" % "1.14",
  "org.scalactic" %% "scalactic" % "2.2.0" % "test",
  "org.scalatest" %% "scalatest" % "2.2.0" % "test",
  "org.scalatestplus" %% "play" % "1.2.0" % "test",
  "org.mockito" % "mockito-core" % "1.9.5" % "test"
)

resolvers += "Local Maven Repository" at "file:///home/michel/.m2/repository/"

