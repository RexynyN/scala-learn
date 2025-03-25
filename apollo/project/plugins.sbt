addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("nl.gn0s1s" % "sbt-dotenv" % "3.1.1")

addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "2.0.8")

classpathTypes += "maven-plugin"

libraryDependencies += "org.bytedeco" % "javacpp" % "1.5.11"