import sbt._
import Keys._

object CoreBuild extends Build {
  lazy val core = Project(id = "signal-collect", base = file("."))
}