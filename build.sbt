ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.2"

lazy val kyoVersion = "0.14.0"

lazy val root = (project in file(".")).settings(
  name := "finance-api",
  idePackagePrefix := Some("fr.corpauration.finance"),
  libraryDependencies ++= Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.11.8",
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.11.8",
    "com.softwaremill.sttp.tapir" %% "tapir-netty-server-zio" % "1.11.9",
    "io.circe"                    %% "circe-core" % "0.14.9",
    "io.circe"                    %% "circe-parser" % "0.14.9",
    "dev.zio"                     %% "zio" % "2.1.12",
    "org.postgresql"               % "postgresql" % "42.7.4"
  )
)
