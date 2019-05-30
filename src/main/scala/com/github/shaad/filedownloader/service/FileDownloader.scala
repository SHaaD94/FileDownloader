package com.github.shaad.filedownloader.service

import java.io.FileOutputStream
import java.net.URI
import java.nio.file.{ Files, Path }

import com.github.shaad.filedownloader._

import scala.concurrent.{ ExecutionContext, Future }

trait FileDownloader {
  def download(url: URI, tempFile: Path, resultFile: Path)(implicit context: ExecutionContext): Future[FileDownloadResult]
}

abstract class FileDownloaderBase extends FileDownloader with WithLogger {
  override final def download(url: URI, tempFile: Path, resultFile: Path)(implicit context: ExecutionContext): Future[FileDownloadResult] = {
    log.info(s"Downloading file from $url")

    getData(url)
      .flatMap {
        case Left(e) =>
          log.error(s"Failed to download file $url, {}", e match {
            case FileNotFound => "file does not exist"
            case OtherError(errorText) => errorText
            case unknownError => unknownError
          })

          Future(DownloadFailed(e))
        case Right(stream) =>
          val writer = {
            val file = tempFile.toFile
            file.delete()
            file.createNewFile()
            new FileOutputStream(file)
          }

          def readStream(): Future[FileDownloadResult] =
            stream.next().part.flatMap {
              case Some(buf) =>
                writer.write(buf.array())
                readStream()
              case None =>
                writer.flush()
                writer.close()
                log.info("File {} downloaded successfully", url)
                Future.apply {
                  resultFile.toFile.delete()
                  Files.move(tempFile, resultFile)
                  DownloadSuccessful
                }
            }

          readStream()
      }
  }

  protected def getData(url: URI)(implicit context: ExecutionContext): Future[Either[DownloadError, FileStream]]
}