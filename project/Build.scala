import sbt._
import Keys._

object CoreBuild extends Build {
  lazy val akkaKryo = ProjectRef(file("../akka-kryo-serialization"), id = "akka-kryo-serialization-custom")
  lazy val core = Project(id = "signal-collect", base = file(".")) dependsOn (akkaKryo)
}
