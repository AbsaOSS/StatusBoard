/*
 * Copyright 2024 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
