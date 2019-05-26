package com.github.shaad.filedownloader

import org.slf4j.{Logger, LoggerFactory}

trait WithLogger {
  val log: Logger = LoggerFactory.getLogger(this.getClass)
}
