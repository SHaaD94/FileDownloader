package com.github.shaad.filedownloader

import java.io.File
import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.Executors

import com.github.shaad.filedownloader.service.HttpFileDownloader
import com.google.common.io.Files
import org.mockserver.client._
import org.mockserver.mockserver.MockServer
import org.mockserver.model._
import org.scalatest._

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, ExecutionContext}

class HttpDownloaderTest extends FlatSpec with Matchers with BeforeAndAfter {
  private implicit val context = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  private val server = new MockServer(8080)
  private val client = new MockServerClient("127.0.0.1", 8080)

  private val tempDir = System.getProperty("java.io.tmpdir")
  private val tempLocation = Paths.get(tempDir, "temp", "nice_pic.jpg")
  private val resLocation = Paths.get(tempDir, "res", "nice_pic.jpg")

  before {
    tempLocation.toFile.mkdirs()
    resLocation.toFile.mkdirs()
  }

  after {
    new File(tempDir).deleteOnExit()
  }

  "HttpDownloader" should "download specified file from url" in {
    client
      .reset()
      .when(HttpRequest.request().withPath("/nice_pic.jpg"))
      .respond(HttpResponse.response().withBody(getPic))

    Await.result(new HttpFileDownloader().download(
      new URI("http://127.0.0.1:8080/nice_pic.jpg"), tempLocation, resLocation), Duration.Inf)

    assert(resLocation.toFile.isFile)
    assert(!tempLocation.toFile.isFile)
    assert(Files.equal(resLocation.toFile, new File(getClass.getResource("/huge_pic.jpg").toURI)))
  }

  "HttpDownloader" should "answer not found" in {
    client.reset()

    val res = Await.result(new HttpFileDownloader().download(
      new URI("http://127.0.0.1:8080/nice_pic.jpg"), tempLocation, resLocation), 10.seconds)
    assertResult(DownloadFailed(FileNotFound))(res)
  }

  private def getPic: Array[Byte] = {
    val reader = getClass.getResourceAsStream("/huge_pic.jpg")
    try
      Stream.continually(reader.read).takeWhile(-1 !=).map(_.toByte).toArray
    finally
      reader.close()
  }
}
