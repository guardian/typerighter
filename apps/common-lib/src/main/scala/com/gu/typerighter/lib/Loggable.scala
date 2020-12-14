package com.gu.typerighter.lib

import org.slf4j.{Logger, LoggerFactory}

trait Loggable {
  implicit val log: Logger = LoggerFactory.getLogger(getClass)
}
