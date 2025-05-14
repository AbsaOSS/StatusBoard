package za.co.absa.statusboard.utils

import zio._
import zio.test.Assertion.{equalTo, isUnit}
import zio.test._

object RegexMatcherUnitTests extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("RegexMatcherSuite")(
      test("Match correct one") {
        assertZIO(ZIO.attempt {
          val matcher = RegexMatcher(".*a.*")
          "sentence containing character 'a'" match {
            case matcher() => "yep"
            case _ => "nope"
          }
        }
        )(equalTo("yep"))
      },
      test("Don't match incorrect one") {
        assertZIO(ZIO.attempt {
          val matcher = RegexMatcher(".*a.*")

          "sentence without ever using hindmost letter of english lexicon" match {
            case matcher() => "yep"
            case _ => "nope"
          }
        }
        )(equalTo("nope"))
      },
      test("Match correct one in option") {
        assertZIO(ZIO.attempt {
          val matcher = RegexMatcher(".*a.*")
          Some("sentence containing character 'a'") match {
            case Some(matcher()) => "yep"
            case _ => "nope"
          }
        }
        )(equalTo("yep"))
      },
      test("Don't match incorrect one in option") {
        assertZIO(ZIO.attempt {
          val matcher = RegexMatcher(".*a.*")

          Some("sentence without ever using hindmost letter of english lexicon") match {
            case Some(matcher()) => "yep"
            case _ => "nope"
          }
        }
        )(equalTo("nope"))
      },
      test("Don't match None") {
        assertZIO(ZIO.attempt {
          val matcher = RegexMatcher(".*a.*")
          val value: Option[String] = None
          value match {
            case Some(matcher()) => "yep"
            case _ => "nope"
          }
        }
        )(equalTo("nope"))
      },
    )
  }
}
