import sbt._
import Keys._

object RootBuild extends Build {
  lazy val core = Project(id = "signal-collect-core", base = file("."))
}