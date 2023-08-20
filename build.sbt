ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.11"

lazy val root = (project in file("."))
  .settings(
    name := "metadata",
    idePackagePrefix := Some("org.hawksatlanta.metadata")
  )

// Testing dependencies
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.15" % Test,
  "junit" % "junit" % "4.13.2" % Test,
  "org.scalatestplus" %% "junit-4-13" % "3.2.15.0" % Test
)
