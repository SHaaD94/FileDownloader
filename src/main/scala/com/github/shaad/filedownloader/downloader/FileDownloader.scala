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
    log.info(s"Downloading file from $url")

    getData(url)
      .flatMap {
        case Left(e) =>
          log.error(s"Failed to download file $url, {}", e)
          Task.now(new DownloadFailed(e))
        case Right(obs) =>
          obs.data
            .consumeWith(createConsumer(obs.contentLength, obs.acceptsContinuation, resultFile))
            .doOnFinish({
              case Some(e) => Task.now(log.error(s"Failed to download $url", e))
              case None => Task.now(log.info("{} downloaded successfully", url))
            })
      }
  }

  protected def getData(url: String): Task[Either[String, FileDownloadInfo]]

  private def createConsumer(contentLength: Option[Long],
                             acceptsContinuation: Boolean,
                             resultFile: String) = {
    Consumer.create[ByteBuffer, FileDownloadResult] { (_, _, callback) =>
      new Observer.Sync[ByteBuffer] {
        private lazy val file = File(resultFile).createFile()
        private lazy val writer = file.outputStream()

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
  }
}