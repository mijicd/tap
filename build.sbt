import Dependencies._

inThisBuild(
  List(
    libraryDependencies += compilerPlugin(monadicFor),
    organization := "com.github.mijicd",
    scalaVersion := "2.12.8",
    version := "0.1.0"
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val root = project
  .in(file("."))
  .settings(
    libraryDependencies += zio,
    name := "tap"
  )
