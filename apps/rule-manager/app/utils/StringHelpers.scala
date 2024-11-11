package utils

object StringHelpers {
  def camelToSnakeCase(name: String) = "[A-Z\\d]".r.replaceAllIn(
    name,
    { m =>
      "_" + m.group(0).toLowerCase()
    }
  )
}
