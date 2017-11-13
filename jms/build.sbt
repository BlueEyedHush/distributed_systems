
lazy val SCALA_VERSION = "2.12.0-M3"
lazy val LOGGER = "ch.qos.logback" % "logback-classic" % "1.1.6"
lazy val ARTEMIS = "org.apache.activemq" % "artemis-jms-client" % "1.2.0"
lazy val SCALAC_OPTS = Seq("-Xexperimental")

lazy val common = project.in(file("common"))
  .settings(
    name := "common",
    version := "0.0",
    crossPaths := false,
    scalaVersion := SCALA_VERSION,
    libraryDependencies ++= Seq(LOGGER, ARTEMIS),
    scalacOptions ++= SCALAC_OPTS)

lazy val producer = project.in(file("producer"))
	.settings(
		name := "producer",
		version := "0.0",
		crossPaths := false,
    scalaVersion := SCALA_VERSION,
    libraryDependencies += LOGGER,
    scalacOptions ++= SCALAC_OPTS)
  .dependsOn(common)

lazy val solver = project.in(file("solver"))
  .settings(
    name := "solver",
    version := "0.0",
    crossPaths := false,
    scalaVersion := SCALA_VERSION,
    libraryDependencies += LOGGER,
    scalacOptions ++= SCALAC_OPTS)
  .dependsOn(common)

lazy val listener = project.in(file("listener"))
  .settings(
    name := "listener",
    version := "0.0",
    crossPaths := false,
    scalaVersion := SCALA_VERSION,
    libraryDependencies += LOGGER,
    scalacOptions ++= SCALAC_OPTS)
  .dependsOn(common)


lazy val rozprochy3 = project.in(file("."))
	.settings(
		name := "rozprochy3",
		version := "0.0",
		crossPaths := false,
    scalaVersion := SCALA_VERSION,
    scalacOptions ++= SCALAC_OPTS)
	.aggregate(common, producer)