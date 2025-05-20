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

import io.circe.syntax.EncoderOps
import za.co.absa.statusboard.model.ApiResponse._
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestEnvironment, assert}
import zio.{Scope, ZIO}

object ApiResponseUnitTests extends ConfigProviderSpec {
  private val singleApiResponse = SingleApiResponse("FOO")
  private val singleApiResponseJson =
    """{
      |  "record" : "FOO"
      |}""".stripMargin

  private val multiApiResponse = MultiApiResponse(Seq("FOO", "BAR", "FOOBAR"))
  private val multiApiResponseJson =
    """{
      |  "records" : [
      |    "FOO",
      |    "BAR",
      |    "FOOBAR"
      |  ]
      |}""".stripMargin

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ServiceConfigurationSuite")(
      suite("SingleApiResponse")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(singleApiResponse.asJson)
          } yield assert(serialized.toString)(equalTo(singleApiResponseJson))
        }
      ),
      suite("MultiApiResponse")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(multiApiResponse.asJson)
          } yield assert(serialized.toString)(equalTo(multiApiResponseJson))
        }
      )
    )
  }
}
