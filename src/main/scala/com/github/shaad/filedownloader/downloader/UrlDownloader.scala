package com.github.shaad.filedownloader.downloader

import java.nio.ByteBuffer

import com.github.shaad.filedownloader.{DownloadFailed, DownloadSuccessful, FileDownloadResult, WithLogger}
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.okhttp.monix.OkHttpMonixBackend
import monix.eval.Task
import monix.execution.Ack.Continue
import monix.execution.Scheduler.Implicits.global
import monix.execution.{Ack, CancelableFuture}
import monix.reactive.{Consumer, Observable, Observer}

import scala.reflect.io.File

trait UrlDownloader {
  def download(url: String, resultFile: String): CancelableFuture[FileDownloadResult]
}

abstract class UrlDownloaderBase extends UrlDownloader with WithLogger {
  protected implicit val client: SttpBackend[Task, Observable[ByteBuffer]] = OkHttpMonixBackend()

  override def download(url: String, resultFile: String): CancelableFuture[FileDownloadResult] = {
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

    val a = getData(url)
      .flatMap {
        case Left(e) => Task.now(new DownloadFailed(e))
        case Right(obs) => obs.consumeWith(consumer)
      }
      .doOnFinish({
        case Some(e) => Task.now(log.error("Failed!", e))
        case None => Task.now(log.info("Downloaded!"))
      })

    a.runToFuture
  }

  protected def getData(url: String): Task[Either[String, Observable[ByteBuffer]]]
}