package com.github.shaad.filedownloader

import java.nio.file.Paths

import com.github.shaad.filedownloader.downloader.HttpFileDownloader
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object Main extends App with WithLogger {
  val tempDir = "/tmp"

  val fileURLs =
    List(
      "https://speed.hetzner.de/10MB.bin",
      "https://speed.hetzner.de/1MB.bin",
      "http://www.ovh.net/files/100Mio.dat",
      "https://speed.hetzner.de/100MB.bin"
    )

  val futures = fileURLs.map(
    fileURL => {
      val fileName = fileURL.split("/").last

      val resultFile = Paths.get(tempDir, fileName)

      new HttpFileDownloader().download(fileURL, resultFile.toString)
    }
  )

  val downloadFuture = Task.gather(futures)
    .doOnFinish({
      case Some(e) => Task.now({
        log.error("Something went wrong", e)
        System.exit(1)
      })
      case None => Task.now()
    }).runToFuture

  val failedDownloads = Await.result(downloadFuture, Duration.Inf).filter(_.isInstanceOf[DownloadFailed])

  if (failedDownloads.nonEmpty) {
    log.error(s"Failed to downloads ${failedDownloads.size} files")
    System.exit(1)
  } else {
    log.info("All files downloaded successfully")
  }

}
