import AssemblyKeys._ 

assemblySettings

/** Project */
name := "signal-collect"

version := "2.2.0-SNAPSHOT"

organization := "com.signalcollect"

scalaVersion := "2.11.4"

/** 
 * See https://github.com/sbt/sbt-assembly/issues/123
 */
mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList(ps @ _*) if ps.last == ".DS_Store" => MergeStrategy.discard
    case other => old(other)
  }
}

scalacOptions ++= Seq("-optimize", "-Ydelambdafy:inline", "-Yclosure-elim", "-Yinline-warnings", "-Ywarn-adapted-args", "-Ywarn-inaccessible", "-feature", "-deprecation", "-Xelide-below", "INFO")

//, "-Ylog:icode"
//, "-Ydebug"

assembleArtifact in packageScala := true

parallelExecution in Test := false

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

EclipseKeys.withSource := true

jarName in assembly := "signal-collect-2.2-SNAPSHOT.jar"

/** Dependencies */
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.5" % "compile",
  "com.typesafe.akka" %% "akka-remote" % "2.3.5" % "compile",
  "org.scala-lang" % "scala-library" % scalaVersion.value % "compile",
  "com.github.romix.akka" %% "akka-kryo-serialization-custom" % "0.3.5" % "compile",
  "org.json4s" %% "json4s-native" % "3.2.9",
  "org.java-websocket" % "Java-WebSocket" % "1.3.0" % "compile",
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
