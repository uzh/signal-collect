import AssemblyKeys._ 

assemblySettings

/** Project */
name := "signal-collect"

version := "3.0.2"

organization := "com.signalcollect"

scalaVersion := "2.11.6"

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

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
//, "-Ylog-classpath"

assembleArtifact in packageScala := true

parallelExecution in Test := false

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

EclipseKeys.withSource := true

jarName in assembly := "signal-collect.jar"

/** Dependencies */
libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-library" % scalaVersion.value % "compile",
  "com.typesafe.akka" %% "akka-actor" % "2.3.11" % "compile",
  "com.typesafe.akka" %% "akka-remote" % "2.3.11" % "compile",
  "com.github.romix.akka" %% "akka-kryo-serialization" % "0.3.3" % "compile",
  "org.json4s" %% "json4s-native" % "3.2.11" % "compile",
  "org.java-websocket" % "Java-WebSocket" % "1.3.0" % "compile",
  "org.webjars" % "d3js" % "3.5.5",
  "org.webjars" % "jquery" % "2.1.4",
  "org.webjars" % "reconnecting-websocket" % "23d2fbc",
  "org.webjars" % "intro.js" % "1.0.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % "compile", // 'compile' in order to share TestAnnouncement with other projects.
  "org.scalacheck" %% "scalacheck" % "1.12.2" % "test",
  "org.easymock" % "easymock" % "3.3.1" % "test"
)

resolvers += "Scala-Tools Repository" at "https://oss.sonatype.org/content/groups/scala-tools/"

resolvers += "Sonatype Snapshots Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "Ifi Public" at "https://maven.ifi.uzh.ch/maven2/content/groups/public/"

transitiveClassifiers := Seq("sources")

pomExtra := (
 <url>https://github.com/uzh/signal-collect</url>
 <scm>
   <url>git@github.com:uzh/signal-collect.git</url>
   <connection>scm:git:git@github.com:uzh/signal-collect.git</connection>
 </scm>
 <developers>
   <developer>
     <id>pstutz</id>
     <name>Philip Stutz</name>
     <url>https://github.com/pstutz</url>
   </developer>
   <developer>
     <id>cshapeshifter</id>
     <name>Carol Alexandru</name>
     <url>https://github.com/cshapeshifter</url>
   </developer>
   <developer>
     <id>troxler</id>
     <name>Silvan Troxler</name>
     <url>https://github.com/troxler</url>
   </developer>
   <developer>
     <id>danistrebel</id>
     <name>Daniel Strebel</name>
     <url>https://github.com/danistrebel</url>
   </developer>
   <developer>
     <id>elaverman</id>
     <name>Mihaela Verman</name>
     <url>https://github.com/elaverman</url>
   </developer>
   <developer>
     <id>lorenzfischer</id>
     <name>Lorenz Fischer</name>
     <url>https://github.com/lorenzfischer</url>
   </developer>
   <developer>
     <id>tmsklr</id>
     <name>Thomas Keller</name>
     <url>https://github.com/tmsklr</url>
   </developer>
   <developer>
     <id>bibekp</id>
     <name>Bibek Paudel</name>
     <url>https://github.com/bibekp</url>
   </developer>
   <developer>
     <id>rampalli-github</id>
     <name>S. Rampalli</name>
     <url>https://github.com/rampalli-github</url>
   </developer>
 </developers>)
 
