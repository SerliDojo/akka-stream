name := "akkastreams"

version := "1.0"

scalaVersion := "2.12.0"

val akkaVersion = "2.4.12"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion
)
    