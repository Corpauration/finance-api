ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.2"

lazy val kyoVersion = "0.14.0"

lazy val root = (project in file("."))
  .settings(
    name := "finance-api",
    idePackagePrefix := Some("fr.corpauration.finance"),
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.11.8",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.11.8",
      "io.circe" %% "circe-core" % "0.14.9",
      "io.circe" %% "circe-parser" % "0.14.9",
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC5",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC5",
      "io.getkyo" %% "kyo-prelude" % kyoVersion,
      "io.getkyo" %% "kyo-core" % kyoVersion,
      "io.getkyo" %% "kyo-direct" % kyoVersion,
      "io.getkyo" %% "kyo-combinators" % kyoVersion,
      "io.getkyo" %% "kyo-sttp" % kyoVersion,
      "io.getkyo" %% "kyo-tapir" % kyoVersion,
      "io.getkyo" %% "kyo-cats" % kyoVersion

    ),
    scalacOptions ++= Seq(
      "-Wvalue-discard",
      "-Wnonunit-statement",
      "-Wconf:msg=(discarded.*value|pure.*statement):error")
  )
