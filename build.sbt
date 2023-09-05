ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.11"
ThisBuild / mainClass    := Some( "org.hawksatlanta.metadata.Main" )

lazy val root = ( project in file( "." ) )
  .settings(
    name             := "metadata",
    idePackagePrefix := Some( "org.hawksatlanta.metadata" )
  )

// Strategy to solve duplicate files in the assembly process
assembly / assemblyMergeStrategy := {
  case PathList( "META-INF", _* ) => MergeStrategy.discard
  case _                          => MergeStrategy.first
}

// Testing dependencies
libraryDependencies ++= Seq(
  "org.scalatest"     %% "scalatest"    % "3.2.15"   % Test,
  "junit"              % "junit"        % "4.13.2"   % Test,
  "io.rest-assured"    % "rest-assured" % "5.3.0"    % Test,
  "org.scalatestplus" %% "junit-4-13"   % "3.2.15.0" % Test
)

// Migration dependencies
libraryDependencies ++= Seq(
  "org.flywaydb" % "flyway-core" % "9.16.0"
)

// Database connection dependencies
libraryDependencies ++= Seq(
  "com.zaxxer"     % "HikariCP"   % "5.0.1",
  "org.postgresql" % "postgresql" % "42.5.4"
)

// HTTP dependencies
libraryDependencies ++= Seq(
  "com.lihaoyi" %% "cask" % "0.9.0"
)

// Validation libraries
libraryDependencies ++= Seq(
  "com.wix" %% "accord-core" % "0.7.6"
)
