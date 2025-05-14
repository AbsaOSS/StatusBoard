package za.co.absa.statusboard.utils

import scala.util.matching.Regex

object JinjaLikeTemplating {
  def renderTemplate(template: String, vars: Map[String, String], ignoreMismatch: Boolean = false): String = {
    val pattern: Regex = """\{\{\s*(\w+)\s*\}\}""".r

    def handleMatch(regMatch: Regex.Match): String = {
      val key = regMatch.group(1)
      vars.get(key) match {
        case Some(value) => Regex.quoteReplacement(value)
        case None =>
          if (ignoreMismatch) regMatch.group(0)
          else throw new NoSuchElementException(s"Unrecognized keyword: $key")
      }
    }

    pattern.replaceAllIn(template, m => handleMatch(m))
  }
}
