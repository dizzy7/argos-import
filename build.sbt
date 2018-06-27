name := "argos-import"

version := "0.1"

scalacOptions += "-Ypartial-unification" // 2.11.9+

scalaVersion := "2.12.6"

val versions = Map(
  "akka" -> "2.5.13",
  "circe" -> "0.9.3",
  "doobie" -> "0.5.3",
  "logback-classic" -> "1.1.3",
  "scala-logging" -> "3.9.0",
  "akka-rabbitmq" -> "5.0.0",
  "amqp-client" -> "5.3.0",
  "pureconfig" -> "0.9.1"
)

libraryDependencies += "ch.qos.logback" % "logback-classic" % versions("logback-classic") % Runtime
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % versions("scala-logging")

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

libraryDependencies += "com.rabbitmq" % "amqp-client" % versions("amqp-client")

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
