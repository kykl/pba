name := """chat-cluster"""

version := "0.1"

scalaVersion := "2.11.7"

lazy val akkaVersion = "2.4.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % "0.5.38",
  "io.grpc" % "grpc-all" % "1.0.0",
  "org.scalatest" %% "scalatest" % "2.1.6" % "test")

import com.trueaccord.scalapb.{ScalaPbPlugin => PB}

PB.protobufSettings

scalaSource in PB.protobufConfig := sourceManaged.value
