package com.github.shaad.filedownloader

import java.io.File
import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.Executors

import com.github.shaad.filedownloader.downloader.{FtpFileDownloader, HttpFileDownloader}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Main extends App with WithLogger {
  val usage =
    """Usage:
      |java -jar file_downloader.jar --temp-dir=absolute_path --result-dir=absolute_path urls
    """.stripMargin

  val tempDir = args.filter(_.startsWith("--temp-dir=")) match {
    case r if r.length != 1 =>
      println(usage)
      System.exit(1).asInstanceOf[String]
    case r => r(0).split('=').last
  }

  val resDir = args.filter(_.startsWith("--result-dir=")) match {
    case r if r.length != 1 =>
      println(usage)
      System.exit(1).asInstanceOf[String]
    case r => r(0).split('=').last
  }

  new File(tempDir).mkdirs()
  new File(resDir).mkdirs()

  implicit val context = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors() * 4))

  val futures: List[Future[FileDownloadResult]] =
    args
      .filter(a => !a.startsWith("--"))
      .toList
      .map(new URI(_))
      .map(uri => {
        val fileName = uri.getPath.split("/").last

        val tempFile = Paths.get(tempDir, fileName)
        val resFile = Paths.get(resDir, fileName)

        uri.getScheme match {
          case "http" | "https" => new HttpFileDownloader().download(uri, tempFile, resFile)
          case "ftp" | "ftps" => new FtpFileDownloader().download(uri, tempFile, resFile)
          case _ =>
            log.error(s"Schema not supported ${uri.getScheme}")
            Future(DownloadFailed(ProtocolNotSupported(uri.getScheme)))
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