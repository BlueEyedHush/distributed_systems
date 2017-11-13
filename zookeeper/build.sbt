
lazy val commonDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.6",
  "org.apache.zookeeper" % "zookeeper" % "3.4.8"
)

lazy val rozprochy6 = project.in(file("."))
	.settings(
		name := "rozprochy6",
		organization := "knawara.rozprochy.zad6",
		version := "0.1",
    libraryDependencies ++= commonDependencies,
		crossPaths := false,
		autoScalaLibrary := false
	)
