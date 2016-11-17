import com.typesafe.sbt.packager.docker._

lazy val root = (project in file(".")).
  settings(
    name := "pba-tracking",
    version := "0.1.11",
    mainClass in Compile := Some("io.bigfast.tracking.grpc.service.TrackingService"),
    scalaVersion := "2.11.8"
  ).
  enablePlugins(JavaAppPackaging)

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("com", "google", "protobuf", xs @ _*) => MergeStrategy.first
    case PathList("org", "apache", "spark", "unused", xs @ _*) => MergeStrategy.first
    case "META-INF/io.netty.versions.properties" => MergeStrategy.first
    case x => old(x)
  }
}

packageName in Docker := "kykl/pba-tracking"
dockerBaseImage := "develar/java:8u45"
dockerCommands := dockerCommands.value flatMap {
  case cmd@Cmd("FROM", _) => List(cmd, Cmd("RUN", "apk update && apk add bash"))
  case other              => List(other)
}

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.12",
  "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % "0.5.42",
  "io.grpc" % "grpc-all" % "1.0.1",
  "org.apache.spark" %% "spark-streaming" % "2.0.1" % "provided",
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.0.1",
//  "io.netty" % "netty-tcnative-boringssl-static" % "1.1.33.Fork22",
  "org.scalatest" %% "scalatest" % "2.1.6" % "test")

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)
