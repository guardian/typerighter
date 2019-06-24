package services

import org.languagetool.Language
import java.util.regex.Pattern
import org.languagetool.rules.Rule
import java.lang.reflect.Constructor

/**
  * Matches 'regexp' elements from XML rules against sentences. Because a version of LanguageTool
  * in which RegexPatternRule is public is not yet available (see
  * https://github.com/languagetool-org/languagetool/pull/1662), we use reflection to gain access
  * to the class here.
  */
object LTRegexPatternRule {

  def getInstance(id: String,
                  description: String,
                  message: String,
                  suggestionsOutMsg: String,
                  language: Language,
                  regex: Pattern,
                  regexpMark: Int) = {
    val c = Class.forName("org.languagetool.rules.patterns.RegexPatternRule")
    val constructor = c.getDeclaredConstructor(classOf[String], classOf[String], classOf[String], classOf[String], classOf[Language], classOf[Pattern], classOf[Int])
    constructor.setAccessible(true)
    val rule = constructor.newInstance(id,
      description,
      message,
      suggestionsOutMsg,
      language,
      regex,
      Int.box(regexpMark))
    rule.asInstanceOf[Rule]
  }
}
