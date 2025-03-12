name := "Apollo"
version := "0.1"
scalaVersion := "2.12.18"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint")

val javacppVersion = "1.5.11"

// Platform classifier for native library dependencies
val platform = org.bytedeco.javacpp.Loader.Detector.getPlatform

// JavaCPP-Preset libraries with native dependencies
val presetLibs = Seq(
  "opencv" -> "4.10.0",
  "ffmpeg" -> "7.1",
  "openblas" -> "0.3.28"
).flatMap { case (lib, ver) =>
  Seq(
    "org.bytedeco" % lib % s"$ver-$javacppVersion",
    "org.bytedeco" % lib % s"$ver-$javacppVersion" classifier platform
  )
}

// Dependências do Spark e OpenCV
libraryDependencies ++= Seq(
  // Sparky 
  "org.apache.spark" %% "spark-core" % "3.0.0",
  "org.apache.spark" %% "spark-sql" % "3.0.0",
  "org.apache.spark" %% "spark-mllib" % "3.0.0",
  "org.apache.spark" %% "spark-streaming" % "3.0.0",

  // To work with sqlite in Sparky
  "org.xerial" % "sqlite-jdbc" % "3.40.1.0",
  
  // Scala Toolkit
  "com.lihaoyi" %% "upickle" % "4.1.0", 
  "com.lihaoyi" %% "os-lib" % "0.11.3",
  "com.softwaremill.sttp.client4" %% "core" % "4.0.0-RC1",
  
  // To work with json files
  "com.github.pathikrit" %% "better-files" % "3.9.2"
) ++ presetLibs

// Configuração do Spark
enablePlugins(AssemblyPlugin)
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _ @_*) => MergeStrategy.discard
  case _                           => MergeStrategy.first
}

// resolvers ++= Resolver.sonatypeOssRepos("snapshots")

autoCompilerPlugins := true
// fork a new JVM for 'run' and 'test:run'
fork := true
// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx1G"
