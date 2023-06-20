package utils

case class NotFoundException(message: String) extends Exception(message)
case class DbException(message: String) extends Exception(message)
