package com.github.shaad.filedownloader.downloader

import java.io.InputStream
import java.net.URI
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import com.github.shaad.filedownloader.{DownloadError, FileNotFound, OtherError, WithLogger}
import org.apache.commons.net.ftp.{FTP, FTPClient}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class FtpFileDownloader extends FileDownloaderBase with WithLogger {
  private val ftpClient = new FTPClient()

  override protected def getData(uri: URI)(implicit context: ExecutionContext): Future[Either[DownloadError, FileStream]] = Future {
    connect(uri) match {
      case Left(e) => Left(e)
      case _ => ftpClient.retrieveFileStream(uri.getPath) match {
        case stream if stream == null => Left(FileNotFound)
        case stream: InputStream => Right(
          FileStream.generate(
            stream,
            supportsRanges = true,
            uri,
            log,
            recover))
      }
    }
  }

  private def connect(uri: URI): Either[DownloadError, Unit] = {
    if (uri.getPort == -1) {
      ftpClient.connect(uri.getHost)
    } else {
      ftpClient.connect(uri.getHost, uri.getPort)
    }

    if (ftpClient.getReplyCode > 299) {
      return Left(new OtherError(ftpClient.getReplyString))
    }

    if (uri.getUserInfo != null) {
      val info = uri.getUserInfo.split(':')
      ftpClient.login(info.apply(0), info.apply(1))
    } else {
      ftpClient.login("anonymous", "")
    }

    if (ftpClient.getReplyCode > 299) {
      return Left(new OtherError(ftpClient.getReplyString))
    }

    ftpClient.enterLocalPassiveMode()
    ftpClient.setBufferSize(8 * 1024)
    ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
    Right()
  }

  private def recover(uri: URI, bytesRead: AtomicLong, currentStream: AtomicReference[InputStream]): Try[Unit] = Try {
    connect(uri)
    ftpClient.setRestartOffset(bytesRead.get())
    currentStream.set(ftpClient.retrieveFileStream(uri.getPath))
  }
}
