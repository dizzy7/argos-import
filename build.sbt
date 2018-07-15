name := "argos-import"

version := "0.1"

scalacOptions += "-Ypartial-unification" // 2.11.9+

scalaVersion := "2.12.6"

val versions = Map(
  "alpakka" -> "0.20",
  "akka" -> "2.5.14",
  "akka-rabbitmq" -> "5.0.0",
  "circe" -> "0.9.3",
  "doobie" -> "0.5.3",
  "logback-classic" -> "1.1.3",
  "scala-logging" -> "3.9.0",
  "pureconfig" -> "0.9.1"
)

libraryDependencies += "ch.qos.logback" % "logback-classic" % versions("logback-classic") % Runtime
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % versions("scala-logging")

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.13"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.13"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-optics"
).map(_ % versions("circe"))

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core",
  "org.tpolecat" %% "doobie-hikari",
  "org.tpolecat" %% "doobie-postgres",
).map(_ % versions("doobie"))

libraryDependencies += "com.github.pureconfig" %% "pureconfig" % versions("pureconfig")

libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-amqp" % versions("alpakka")

mainClass in assembly := Some("app.Application")
assemblyJarName in assembly := "argos-import.jar"
