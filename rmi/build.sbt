
lazy val commonDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.6"
)

lazy val common = project.in(file("common"))
  .settings(
    name := "common",
		organization := "knawara.zad2.common",
    version := "0.1",
		crossPaths := false,
		autoScalaLibrary := false
  )

lazy val server = project.in(file("server"))
	.settings(
		name := "server",
		organization := "knawara.zad2.server",
		version := "0.1",
    libraryDependencies ++= commonDependencies,
		crossPaths := false,
		autoScalaLibrary := false,

		mainClass in assembly := Some("knawara.zad2.server.Main")
	)
  .dependsOn(common)

lazy val client = project.in(file("client"))
	.settings(
		name := "client",
		organization := "knawara.zad2.client",
		version := "0.1",
    libraryDependencies ++= commonDependencies,
		crossPaths := false,
		autoScalaLibrary := false,

		mainClass in assembly := Some("knawara.zad2.client.ClientMain")
	)
	.dependsOn(common)

lazy val noughtsCrosses = project.in(file("."))
	.settings(
		name := "noughtsAndCrosses",
		organization := "knawara.zad2",
		version := "0.1"
	)
	.aggregate(common, server, client)