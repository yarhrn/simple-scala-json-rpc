import sbt.Keys.libraryDependencies
import ReleaseTransformations._

name := "simple-scala-json-rpc"


releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies, // : ReleaseStep
  inquireVersions, // : ReleaseStep
  runClean, // : ReleaseStep
  runTest, // : ReleaseStep
  setReleaseVersion, // : ReleaseStep
  commitReleaseVersion, // : ReleaseStep, performs the initial git checks
  tagRelease, // : ReleaseStep
  setNextVersion, // : ReleaseStep
  commitNextVersion, // : ReleaseStep
  pushChanges // : ReleaseStep, also checks that an upstream branch is properly configured
)

val common = List(
  organization := "loco",
  scalaVersion := "2.13.6",
)
inThisBuild(common)


val publishing = List(
  organization := "com.yarhrn",
  homepage := Some(url("https://github.com/yarhrn/simple-scala-json-rpc")),
  scmInfo := Some(ScmInfo(url("https://github.com/yarhrn/simple-scala-json-rpc"), "git@github.com:yarhrn/simple-scala-json-rpc.git")),
  developers := List(Developer("Yaroslav Hryniuk",
    "Yaroslav Hryniuk",
    "yaroslavh.hryniuk@gmail.com",
    url("https://github.com/yarhrn"))),
  licenses += ("MIT", url("https://github.com/yarhrn/simple-scala-json-rpc/blob/main/LICENSE")),
  publishMavenStyle := true
)

val http4sVersion = "0.23.11"

lazy val sttp = (project in file("sttp")).dependsOn(core)
  .settings(
    name := "sttp",
    publishing,
    //    libraryDependencies += "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.5.2",
    //    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.3.11",
    libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.5.2",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0" % Test,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % http4sVersion % Test,
      "org.http4s" %% "http4s-ember-server" % http4sVersion % Test,
      "org.http4s" %% "http4s-ember-client" % http4sVersion % Test,
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.5.2"  % Test
    )
  )

lazy val core = (project in file("core")).settings(
  name := "core",
  publishing,
  libraryDependencies += "org.typelevel" %% "cats-core" % "2.7.0",
  libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.1"
)