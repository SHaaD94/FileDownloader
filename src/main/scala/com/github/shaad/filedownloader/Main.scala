package com.github.shaad.filedownloader

import java.io.File
import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.Executors

import com.github.shaad.filedownloader.downloader.{ FtpFileDownloader, HttpFileDownloader }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

object Main extends App with WithLogger {
  val tempDir = "/tmp/downloads/temp"
  val resDir = "/tmp/downloads/res"

  new File(tempDir).mkdirs()
  new File(resDir).mkdirs()

  val fileURLs =
    List(
      //      "http://www.clker.com/cliparts/0/f/d/b/12917289761851255679earth-map-huge-md.png",
      "http://lhcb-reconstruction.web.cern.ch/lhcb-reconstruction/Augenblick/Pictures/72252_71060817_0_huge.jpg",
      "ftp://speedtest.tele2.net/20MB.zip",
      //      "ftp://speedtest.tele2.net/200MB.zip",
      "ftp://speedtest.tele2.net/500MB.zip",
      "ftp://speedtest:speedtest@ftp.otenet.gr/test10Mb.db",
      "http://www.ovh.net/files/100Mio.dat",
      //      "http://www.ovh.net/files/1Gio.dat",
      //      "https://speed.hetzner.de/10GB.bin",
      //      "https://speed.hetzner.de/1GB.bin",
      "https://speed.hetzner.de/100MB.bin")

  implicit val context = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors() * 4))

  val futures = fileURLs
    .map(new URI(_))
    .map(uri => {
      val fileName = uri.getPath.split("/").last

      val tempFile = Paths.get(tempDir, fileName)
      val resFile = Paths.get(resDir, fileName)

      uri.getScheme match {
        case "http" | "https" => new HttpFileDownloader().download(uri, tempFile, resFile)
        case "ftp" | "ftps" => new FtpFileDownloader().download(uri, tempFile, resFile)
        case _ => Future(ProtocolNotSupported(uri.getScheme))
        // extend protocols here
      }
    })

  Future.sequence(futures)
    .onComplete {
      case Success(downloadedFiles) =>
        val failedDownloads = downloadedFiles
          .filter(_.isInstanceOf[DownloadFailed])
        if (failedDownloads.nonEmpty) {
          log.error(s"Failed to download ${failedDownloads.size} files")
          System.exit(1)
        } else {
          log.info("All files downloaded successfully")
          System.exit(0)
        }
      case Failure(exception) =>
        log.error("Something went wrong", exception)
        System.exit(1)
    }
}
