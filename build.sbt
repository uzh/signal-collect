import AssemblyKeys._ 

assemblySettings

/** Project */
name := "signal-collect"

version := "2.0.0-SNAPSHOT"

organization := "com.signalcollect"

scalaVersion := "2.10.0-RC2"

parallelExecution in Test := false

EclipseKeys.withSource := true

/** Dependencies */
libraryDependencies ++= Seq(
 "com.typesafe.akka" % "akka-actor_2.10.0-RC2" % "2.1.0-RC2" ,
 "com.typesafe.akka" % "akka-remote_2.10.0-RC2" % "2.1.0-RC2" ,
 "org.scala-lang" % "scala-library" % "2.10.0-RC2" % "compile",
 "com.esotericsoftware.kryo" % "kryo" % "2.20" % "compile",
 "org.tukaani" % "xz" % "1.1" % "compile",
 "ch.ethz.ganymed" % "ganymed-ssh2" % "build210"  % "compile",
 "commons-codec" % "commons-codec" % "1.7"  % "compile",
 "junit" % "junit" % "4.8.2"  % "test",
 "org.specs2" % "specs2_2.10.0-RC2" % "1.12.2"  % "test",
 "org.mockito" % "mockito-all" % "1.9.0"  % "test"
  )