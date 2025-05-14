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
import zio.test.Assertion.equalTo
import zio.test._

object JinjaLikeTemplatingUnitTests extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("JinjaLikeTemplatingSuite")(
      test("Match replaces properly") {
        for {
          template <- ZIO.succeed( """create table {{ns}}.{{table}}.{{ns}}
                                     |{{ns}}.{{foo}}""".stripMargin)
          vars <- ZIO.succeed(Map("ns" -> "lhd", "table" -> "core", "foo" -> "bar"))
          expected <- ZIO.succeed("""create table lhd.core.lhd
              |lhd.bar""".stripMargin)
          result <- ZIO.succeed(JinjaLikeTemplating.renderTemplate(template, vars))
        } yield assert(result)(equalTo(expected))
      }
    )
  }
}
