package com.github.shaad.filedownloader

import java.nio.ByteBuffer

import monix.reactive.Observable

sealed class FileDownloadResult

class DownloadSuccessful extends FileDownloadResult

class DownloadFailed(val error: String) extends FileDownloadResult


case class FileDownloadInfo(contentLength: Option[Long], acceptsContinuation: Boolean, data: Observable[ByteBuffer])

