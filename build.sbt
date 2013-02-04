import AssemblyKeys._ 

assemblySettings

/** Project */
name := "signal-collect"

version := "2.1.0-SNAPSHOT"

organization := "com.signalcollect"

scalaVersion := "2.10.0"

assembleArtifact in packageScala := false

parallelExecution in Test := false

EclipseKeys.withSource := true

jarName in assembly := "signal-collect-2.1-SNAPSHOT.jar"

test in assembly := {}

/** Dependencies */
libraryDependencies ++= Seq(
 "com.typesafe.akka" % "akka-actor_2.10" % "2.1.0" ,
 "com.typesafe.akka" % "akka-remote_2.10" % "2.1.0" ,
 "org.scala-lang" % "scala-library" % "2.10.0" % "compile",
 "com.esotericsoftware.kryo" % "kryo" % "2.20" % "compile",
 "ch.ethz.ganymed" % "ganymed-ssh2" % "build210"  % "compile",
 "commons-codec" % "commons-codec" % "1.7"  % "compile",
 "junit" % "junit" % "4.8.2"  % "test",
 "org.specs2" % "specs2_2.10" % "1.13"  % "test",
 "org.specs2" % "classycle" % "1.4.1" % "test",
 "org.mockito" % "mockito-all" % "1.9.0"  % "test",
 "org.java_websocket" % "java_websocket" % "1.0.0-SNAPSHOT" % "compile" from "https://github.com/TooTallNate/Java-WebSocket/raw/master/dist/java_websocket.jar"
  )

