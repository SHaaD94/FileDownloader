package com.github.shaad.filedownloader.service

import java.io.InputStream
import java.net.URI
import java.util.concurrent.atomic.{ AtomicLong, AtomicReference }

import com.github.shaad.filedownloader.{ DownloadError, FileNotFound, OtherError }
import okhttp3.OkHttpClient
import okhttp3.Request.Builder

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

class HttpFileDownloader extends FileDownloaderBase {
  private val client = new OkHttpClient.Builder().build()

  override protected def getData(url: URI)(implicit context: ExecutionContext): Future[Either[DownloadError, FileStream]] = Future {
    Try {
      val request = new Builder().get().url(url.toURL).build()
      client.newCall(request).execute() match {
        case res if res.code() == 404 => Left(FileNotFound)
        case res if !res.isSuccessful => Left(OtherError(res.body().string()))
        case res => Right {
          FileStream.generate(
            res.body().byteStream(),
            res.header("Accept-Ranges") match {
              case "bytes" => true
              case _ => false
            },
            url,
            log,
            recover)
        }
      }
    } match {
      case Success(stream) => stream
      case Failure(error) => Left(OtherError(error.getMessage))
    }
  }

  private def recover(uri: URI, bytesRead: AtomicLong, currentStream: AtomicReference[InputStream]): Try[Unit] = Try {
    val request = new Builder().get()
      .url(uri.toURL)
      .header("Range", s"bytes=${bytesRead.get()}-")
      .build()
    client.newCall(request).execute() match {
      case r if r.isSuccessful =>
        currentStream.set(r.body().byteStream())
      case _ => Thread.sleep(5000)
    }
  }
}