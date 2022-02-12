ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.1"

val akkaVersion = "2.6.13"
val akkaHttpVersion = "10.2.7"
val scalaTestVersion = "3.2.11"
val scalaMockVersion = "5.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "com.twogis",
    libraryDependencies ++= Seq(
      // akka streams
      ("com.typesafe.akka" %% "akka-stream" % akkaVersion).cross(CrossVersion.for3Use2_13),
      // akka http
      ("com.typesafe.akka" %% "akka-http" % akkaHttpVersion).cross(CrossVersion.for3Use2_13),
      ("com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion).cross(CrossVersion.for3Use2_13),
      ("com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion).cross(CrossVersion.for3Use2_13),
      // akka classic
      ("com.typesafe.akka" %% "akka-actor" % akkaVersion).cross(CrossVersion.for3Use2_13),
      // testing
      ("com.typesafe.akka" %% "akka-testkit" % akkaVersion).cross(CrossVersion.for3Use2_13),
      ("org.scalamock" %% "scalamock" % scalaMockVersion % Test).cross(CrossVersion.for3Use2_13),
      "org.scalatest" %% "scalatest" % scalaTestVersion,
      // parsing HTML
      "org.jsoup" % "jsoup" % "1.14.3"
    )
)