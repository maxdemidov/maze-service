name := "maze-service"

version := "0.1"

scalaVersion := "2.12.11"

val scalaVers = "2.12.11"
val akkaVers = "2.6.5"
val akkaHttpVers = "10.1.11"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % scalaVers,
  "org.scala-lang" % "scala-library" % scalaVers,
  "org.scala-lang" % "scala-reflect" % scalaVers,

  "com.typesafe.akka" %% "akka-actor" % akkaVers,
  "com.typesafe.akka" %% "akka-stream" % akkaVers,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVers,

  "com.typesafe.akka" %% "akka-testkit" % akkaVers % Test
)