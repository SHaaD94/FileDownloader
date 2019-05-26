package com.github.shaad.filedownloader

import java.nio.file.Paths
import java.util.concurrent.Executors

import com.github.shaad.filedownloader.downloader.HttpUrlDownloader

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


object Main extends App with WithLogger {
  val tempDir = "/tmp"

  //  val fileURL = "ftp://speedtest.tele2.net/10MB.zip"
  //  val fileURL = "https://speed.hetzner.de/100MB.bin"

  val fileURL = "http://lhcb-reconstruction.web.cern.ch/lhcb-reconstruction/Augenblick/Pictures/72252_71060817_0_huge.jpg"

  val fileName = fileURL.split("/").last

  val resultFile = Paths.get(tempDir, fileName)

  implicit val context = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  new HttpUrlDownloader().download(fileURL, resultFile.toString)
    .onComplete({
      case Success(_) =>
        log.info("Finished")
        System.exit(0)

      case Failure(e) =>
        log.error("Failed", e)
        System.exit(1)
    })
}
