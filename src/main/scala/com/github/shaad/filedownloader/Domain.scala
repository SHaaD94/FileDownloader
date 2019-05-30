package com.github.shaad.filedownloader

sealed class FileDownloadResult
object DownloadSuccessful extends FileDownloadResult
case class DownloadFailed(error: DownloadError) extends FileDownloadResult

sealed class DownloadError
object FileNotFound extends DownloadError
case class ProtocolNotSupported(protocol: String) extends DownloadError
case class OtherError(error: String) extends DownloadError
