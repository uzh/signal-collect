import AssemblyKeys._ 

assemblySettings

/** Project */
name := "signal-collect"

version := "2.1.0-SNAPSHOT"

organization := "com.signalcollect"

scalaVersion := "2.11.0-RC1"

scalacOptions ++= Seq("-optimize", "-Yinline-warnings", "-feature", "-deprecation", "-Xelide-below", "INFO" )

assembleArtifact in packageScala := true

excludedJars in assembly <<= (fullClasspath in assembly) map { cp => 
  cp filter {_.data.getName == "minlog-1.2.jar"}
}

parallelExecution in Test := false

scalacOptions += "-deprecation"

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

EclipseKeys.withSource := true

jarName in assembly := "signal-collect-3.0-SNAPSHOT.jar"

/** Dependencies */
libraryDependencies ++= Seq(
	"com.typesafe.akka"			%% "akka-actor"			% "2.3.0-RC4"	% "compile",
	"com.typesafe.akka"			%% "akka-remote"			% "2.3.0-RC4"	% "compile",
	"org.scala-lang"				% "scala-library"		% "2.11.0-RC1"	% "compile",
	"com.esotericsoftware.kryo"	% "kryo"					% "2.21"			% "compile",
	"net.liftweb"				% "lift-json_2.10"		% "2.5-RC4"		% "compile",
	"org.java-websocket"			% "Java-WebSocket"		% "1.3.0"		% "compile",
	"junit"						% "junit"				% "4.8.2"		% "test",
	"org.specs2"					% "classycle"			% "1.4.1"		% "test",
	"org.mockito"				% "mockito-all"			% "1.9.0"		% "test",
	"org.specs2"					%% "specs2"				% "2.3.9"		% "test",
	"org.scalacheck"				%% "scalacheck"			% "1.11.3"		% "test",
	"org.scalatest"				%% "scalatest"			% "2.1.0"		% "test",
	"org.easymock" 				% "easymock"				% "3.2"			% "test"
)

   
    
    


resolvers += "Scala-Tools Repository" at "https://oss.sonatype.org/content/groups/scala-tools/"

resolvers += "Sonatype Snapshots Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"

