package com.github.shaad.filedownloader

sealed class FileDownloadResult

class DownloadSuccessful extends FileDownloadResult

class DownloadFailed(val error: String) extends FileDownloadResult
