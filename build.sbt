name := "pcp-stream"
organization := "io.github.lock-free"
version := "0.0.2"
scalaVersion := "2.12.4"

useGpg := true 
parallelExecution in Test := true

publishTo := sonatypePublishTo.value

libraryDependencies ++= Seq(
  // test suite
  "org.scalatest" %% "scalatest" % "3.0.1" % Test,

  // pcp protocol
  "io.github.lock-free" %% "pcp" % "0.1.1"
)
