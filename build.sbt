import AssemblyKeys._ // put this at the top of the file

assemblySettings

/** Project */
name := "signal-collect-core"

version := "2.0.0-SNAPSHOT"

organization := "com.signalcollect"

scalaVersion := "2.10.0-M7"

parallelExecution in Test := false

/** Dependencies */
libraryDependencies ++= Seq(
 "com.typesafe.akka" % "akka-actor_2.10.0-M7" % "2.1-M2" ,
 "com.typesafe.akka" % "akka-remote_2.10.0-M7" % "2.1-M2" ,
 "org.scala-lang" % "scala-library" % "2.10.0-M7" % "compile",
 "ch.ethz.ganymed" % "ganymed-ssh2" % "build210"  % "compile",
 "commons-codec" % "commons-codec" % "1.7"  % "compile",
 "junit" % "junit" % "4.8.2"  % "test",
 "org.specs2" % "specs2_2.10.0-M7" % "1.12.1.1"  % "test",
 "org.mockito" % "mockito-all" % "1.9.0"  % "test"
  )

