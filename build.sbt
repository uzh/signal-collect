import AssemblyKeys._ 

assemblySettings

/** Project */
name := "signal-collect"

version := "2.1.0-SNAPSHOT"

organization := "com.signalcollect"

scalaVersion := "2.11.1"

scalacOptions ++= Seq("-optimize", "-Yinline-warnings", "-feature", "-deprecation", "-Xelide-below", "INFO" )

assembleArtifact in packageScala := true

parallelExecution in Test := false

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

EclipseKeys.withSource := true

jarName in assembly := "signal-collect-2.1-SNAPSHOT.jar"

/** Dependencies */
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.3" % "compile",
  "com.typesafe.akka" %% "akka-remote" % "2.3.3" % "compile",
  "com.github.romix.akka" %% "akka-kryo-serialization-custom" % "0.3.5" % "compile",
  "org.scala-lang" % "scala-library" % "2.11.1" % "compile",
  "org.json4s" %% "json4s-native" % "3.2.9",
  "org.java-websocket" % "Java-WebSocket" % "1.3.0" % "compile",
  "org.scala-lang.modules" %% "scala-async" % "0.9.1" % "compile",
  "junit" % "junit" % "4.8.2"  % "test",
  "org.specs2" % "classycle" % "1.4.1" % "test",
  "org.mockito" % "mockito-all" % "1.9.0"  % "test",
  "org.specs2" %% "specs2" % "2.3.11"  % "test",
  "org.scalacheck" %% "scalacheck" % "1.11.3" % "test",
  "org.scalatest" %% "scalatest" % "2.1.3" % "test",
  "org.easymock" % "easymock" % "3.2" % "test"
)

resolvers += "Scala-Tools Repository" at "https://oss.sonatype.org/content/groups/scala-tools/"

resolvers += "Sonatype Snapshots Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "Ifi Public" at "https://maven.ifi.uzh.ch/maven2/content/groups/public/"

transitiveClassifiers := Seq("sources")
