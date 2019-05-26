package com.github.shaad.filedownloader.downloader

import java.nio.ByteBuffer

import com.github.shaad.filedownloader._
import monix.eval.Task
import monix.execution.Ack
import monix.execution.Ack.Continue
import monix.reactive.{Consumer, Observer}

import scala.reflect.io.File

trait UrlDownloader {
  def download(url: String, resultFile: String): Task[FileDownloadResult]
}

abstract class UrlDownloaderBase extends UrlDownloader with WithLogger {
  override def download(url: String, resultFile: String): Task[FileDownloadResult] = {
    val file = File(resultFile).createFile()
    val writer = file.outputStream()

    log.info(s"Downloading file from $url")

    val consumer: Consumer[ByteBuffer, FileDownloadResult] = Consumer.create[ByteBuffer, FileDownloadResult] { (_, _, callback) =>
      new Observer.Sync[ByteBuffer] {
        def onNext(buffer: ByteBuffer): Ack = {
          writer.write(buffer.array())
          Continue
        }

        def onComplete(): Unit = {
          writer.flush()
          writer.close()
          callback.onSuccess(new DownloadSuccessful())
        }

        def onError(ex: Throwable): Unit = {
          writer.flush()
          writer.close()
          callback.onError(ex)
        }
      }
    }

    getData(url)
      .flatMap {
        case Left(e) =>
          log.error(s"Failed to download file $url, {}", e)
          Task.now(new DownloadFailed(e))
        case Right(obs) =>
          obs.data
            .consumeWith(consumer)
            .doOnFinish({ case None => Task.now(log.info(s"$url downloaded successfully")) })
      }
  }

  protected def getData(url: String): Task[Either[String, FileDownloadInfo]]
}