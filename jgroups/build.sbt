
lazy val commonDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.6"
)

lazy val client = project.in(file("."))
	.settings(
		name := "client",
		organization := "knawara.zad2.client",
		version := "0.1",
    libraryDependencies ++= commonDependencies,
		crossPaths := false,
		autoScalaLibrary := false
	)