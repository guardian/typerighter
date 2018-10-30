package utils

import org.slf4j.{Logger, LoggerFactory}

trait Loggable {
  implicit val logger: Logger = LoggerFactory.getLogger(getClass)
}
