package com.github.shaad.filedownloader.downloader

import java.nio.ByteBuffer

import com.softwaremill.sttp._
import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.duration.Duration

class HttpUrlDownloader extends UrlDownloaderBase {
  override protected def getData(url: String): Task[Either[String, Observable[ByteBuffer]]] = {
    sttp
      .get(uri"$url")
      .response(asStream[Observable[ByteBuffer]])
      .readTimeout(Duration.Inf)
      .send()
      .map(_.body)
  }
}
