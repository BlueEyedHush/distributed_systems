
lazy val chat = project.in(file("."))
	.settings(
		name := "chat",
		organization := "knawara.zad1.chat",
		version := "0.1",

		javacOptions in (Compile, compile) ++= Seq("-source", "1.8", "-target", "1.8"),
		autoScalaLibrary := false,
		crossPaths := false,
    	libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.6",

		mainClass in assembly := Some("knawara.zad2.client.ChatMain")
	)