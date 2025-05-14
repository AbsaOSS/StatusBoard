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

package za.co.absa.statusboard.model

import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestEnvironment, assert}
import zio.{Scope, ZIO}

object NotificationConditionUnitTests extends ConfigProviderSpec {
  private val durationBased: NotificationCondition = NotificationCondition.DurationBased(5)
  private val durationBasedJson =
    """{
      |  "DurationBased" : {
      |    "secondsInState" : 5
      |  }
      |}""".stripMargin

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ServiceConfigurationSuite")(
      suite("DurationBased")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(durationBased.asJson)
          } yield assert(serialized.toString)(equalTo(durationBasedJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(durationBasedJson))
            deserialized <- ZIO.fromEither(jsonObject.as[NotificationCondition])
          } yield assert(deserialized)(equalTo(durationBased))
        }
      )
    )
  }
}
