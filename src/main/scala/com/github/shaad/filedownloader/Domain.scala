package com.github.shaad.filedownloader

sealed class FileDownloadResult
object DownloadSuccessful extends FileDownloadResult
class DownloadFailed(error: DownloadError) extends FileDownloadResult

sealed class DownloadError
object FileNotFound extends DownloadError
class OtherError(val error: String) extends DownloadError
