
lazy val icedemo = project.in(file("."))
	.settings(
		name := "icedemo", 
		version := "0.1", 
		organization := "sr.ice",
		javacOptions in (Compile, compile) ++= Seq("-source", "1.8", "-target", "1.8", "-g:lines"),
		crossPaths := false,
		autoScalaLibrary := false,
		unmanagedSourceDirectories in Compile <+= baseDirectory( _ / "src" / "generated" / "java"),
		
		resolvers += "ZeroC Releases" at "https://repo.zeroc.com/nexus/content/repositories/releases/",
		libraryDependencies += "com.zeroc" % "ice" % "3.5.1",
		libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7",
		libraryDependencies += "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.4.2")