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
import za.co.absa.statusboard.model.ErrorResponse._
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestEnvironment, assert}
import zio.{Scope, ZIO}

object ErrorResponseUnitTests extends ConfigProviderSpec {
  private val badRequestResponse = BadRequestResponse("BAKA")
  private val unauthorizedErrorResponse = UnauthorizedErrorResponse("BAKA")
  private val recordNotFoundErrorResponse = RecordNotFoundErrorResponse("BAKA")
  private val dataConflictErrorResponse = DataConflictErrorResponse("BAKA")
  private val internalServerErrorResponse = InternalServerErrorResponse("BAKA")
  private val commonJson =
    """{
      |  "message" : "BAKA"
      |}""".stripMargin

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ServiceConfigurationSuite")(
      suite("BadRequestResponse")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(badRequestResponse.asJson)
          } yield assert(serialized.toString)(equalTo(commonJson))
        }
      ),
      suite("UnauthorizedErrorResponse")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(unauthorizedErrorResponse.asJson)
          } yield assert(serialized.toString)(equalTo(commonJson))
        }
      ),
      suite("RecordNotFoundErrorResponse")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(recordNotFoundErrorResponse.asJson)
          } yield assert(serialized.toString)(equalTo(commonJson))
        }
      ),
      suite("DataConflictErrorResponse")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(dataConflictErrorResponse.asJson)
          } yield assert(serialized.toString)(equalTo(commonJson))
        }
      ),
      suite("InternalServerErrorResponse")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(internalServerErrorResponse.asJson)
          } yield assert(serialized.toString)(equalTo(commonJson))
        }
      )
    )
  }
}
