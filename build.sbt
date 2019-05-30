lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.github.shaad",
      scalaVersion := "2.12.8"
    )),
    name := "FileDownloader",


    libraryDependencies ++= Seq(
      "com.squareup.okhttp3" % "okhttp" % "3.14.2",
      "commons-net" % "commons-net" % "3.6",

      "ch.qos.logback" % "logback-classic" % "1.2.3",

      "org.scalatest" %% "scalatest" % "3.0.5" % Test,
      "org.mock-server" % "mockserver-netty" % "5.5.4" % Test,
      "org.mock-server" % "mockserver-client-java" % "5.5.4" % Test
    )
  )

mainClass in assembly := some("com.github.shaad.filedownloader.Main")
assemblyJarName := "file_downloader.jar"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.first
}