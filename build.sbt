
lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.github.shaad",
      scalaVersion := "2.12.8"
    )),
    name := "FileDownloader",


    libraryDependencies ++= Seq(
      "io.monix" %% "monix" % "2.3.3",

      "com.softwaremill.sttp" %% "okhttp-backend-monix" % "1.5.17",
      
      "ch.qos.logback" % "logback-classic" % "1.2.3",

      "org.scalatest" %% "scalatest" % "3.0.5" % Test
    )
  )
