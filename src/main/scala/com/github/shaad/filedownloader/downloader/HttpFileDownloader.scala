package com.github.shaad.filedownloader.downloader

import java.nio.ByteBuffer

import com.github.shaad.filedownloader.FileDownloadInfo
import com.softwaremill.sttp._
import com.softwaremill.sttp.okhttp.monix.OkHttpMonixBackend
import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.duration.Duration

class HttpFileDownloader extends UrlDownloaderBase {
  private implicit val client: SttpBackend[Task, Observable[ByteBuffer]] = OkHttpMonixBackend()

  override protected def getData(url: String): Task[Either[String, FileDownloadInfo]] = {
    def getContentLength(res: Response[Observable[ByteBuffer]]): Option[Long] =
      res.header("Content-Length") match {
        case Some(value) => Some(value.toLong)
        case None => None
      }

    def isContinuationAllowed(res: Response[Observable[ByteBuffer]]): Boolean =
      res.header("Accept-Ranges") match {
        case Some(value) => value.equals("bytes")
        case None => false
      }

    sttp
      .get(uri"$url")
      .response(asStream[Observable[ByteBuffer]])
      .readTimeout(Duration.Inf)
      .send()
      .map(res => res.body match {
        case Left(value) => Left(value)
        case Right(obs) => Right(FileDownloadInfo(getContentLength(res), isContinuationAllowed(res), obs))
      })
  }
}
