
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

      "org.scalatest" %% "scalatest" % "3.0.5" % Test
    )
  )
