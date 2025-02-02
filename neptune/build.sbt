name := "Neptune"
version := "0.1"
scalaVersion := "2.12.18"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint")

val javacppVersion = "1.5.11"

// Platform classifier for native library dependencies
val platform = org.bytedeco.javacpp.Loader.Detector.getPlatform

// JavaCPP-Preset libraries with native dependencies
val presetLibs = Seq(
  "opencv"   -> "4.10.0",
  "ffmpeg"   -> "7.1",
  "openblas" -> "0.3.28"
).flatMap { case (lib, ver) =>
  Seq(
    "org.bytedeco" % lib % s"$ver-$javacppVersion",
    "org.bytedeco" % lib % s"$ver-$javacppVersion" classifier platform
  )
}

// Dependências do Spark e OpenCV
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.3.4",
  "org.apache.spark" %% "spark-sql" % "3.5.4",
  "org.apache.spark" %% "spark-mllib" % "3.5.4",
  "org.apache.spark" %% "spark-streaming" % "3.5.4",
  "org.bytedeco"            % "javacpp"         % javacppVersion,
  "org.bytedeco"            % "javacpp"         % javacppVersion classifier platform,
  "org.bytedeco"            % "javacv"          % javacppVersion,
  "org.scala-lang.modules" %% "scala-swing"     % "3.0.0",
  "org.scalafx"            %% "scalafx"         % "23.0.1-R34",
  "org.scalafx"            %% "scalafx-extras"  % "0.10.1",
) ++ presetLibs


// Configuração do Spark
enablePlugins(AssemblyPlugin)
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _ @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

resolvers ++= Resolver.sonatypeOssRepos("snapshots")

autoCompilerPlugins := true
// fork a new JVM for 'run' and 'test:run'
fork := true
// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx1G"