package za.co.absa.statusboard.utils

case class RegexMatcher(regex: String) {
  def unapply(s: String): Boolean = s.matches(regex)
}
