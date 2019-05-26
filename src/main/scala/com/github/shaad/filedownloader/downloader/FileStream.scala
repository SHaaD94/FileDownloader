package com.github.shaad.filedownloader.downloader

import java.io.InputStream
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import org.slf4j.Logger

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class FileStreamPart(part: Future[Option[ByteBuffer]]) {
  object FileStreamPart {
    def apply(futureByteBuffer: Future[Option[ByteBuffer]]) = new FileStreamPart(futureByteBuffer)
  }
}

abstract class FileStream {
  def next(): FileStreamPart
}

object FileStream {
  def generate(inputStream: InputStream,
               supportsRanges: Boolean,
               uri: URI,
               log: Logger,
               recover: (URI, AtomicLong, AtomicReference[InputStream]) => Try[Unit])
              (implicit context: ExecutionContext): FileStream = new FileStream {
    private val bytesRead = new AtomicLong()
    private val currentStream = new AtomicReference(inputStream)
    private val buffer = new Array[Byte](8 * 1024)

    override def next(): FileStreamPart = FileStreamPart(Future {
      @tailrec
      def readFromStream: Int = {
        try {
          currentStream.get().read(buffer)
        } catch {
          case e: Throwable =>
            Try(currentStream.get().close())
            if (!supportsRanges) {
              context.reportFailure(e)
            }
            log.warn("Error occurred ({}), retrying", e.getMessage)
            var updatedStream = false
            while (!updatedStream) {
              recover(uri, bytesRead, currentStream) match {
                case Failure(exception) =>
                  log.warn(s"Failed to restore connection, ({})", exception.getMessage)
                  Thread.sleep(5000)
                case Success(_) =>
                  updatedStream = true
              }
            }
            readFromStream
        }
      }

      val portionSize = readFromStream

      portionSize match {
        case -1 => None
        case _ =>
          val data = new Array[Byte](portionSize)
          System.arraycopy(buffer, 0, data, 0, portionSize)
          bytesRead.addAndGet(portionSize)
          Some(ByteBuffer.wrap(data))
      }
    })
  }

}

