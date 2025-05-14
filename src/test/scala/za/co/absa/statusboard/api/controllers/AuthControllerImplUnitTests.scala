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

package za.co.absa.statusboard.api.controllers

import za.co.absa.statusboard.model.ErrorResponse.UnauthorizedErrorResponse
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio._
import zio.test.Assertion.{failsWithA, isUnit}
import zio.test._

object AuthControllerImplUnitTests extends ConfigProviderSpec {
  private val apiKey = "<api-key-value>" // NOTE: this need to correspond to actual value in test config

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("AuthControllerImplSuite")(
      test("pass on correct key") {
          assertZIO(AuthController.authorize(apiKey))(isUnit)
      },
      test("guard on wrong key") {
          assertZIO(AuthController.authorize("SpoofedKey").exit)(failsWithA[UnauthorizedErrorResponse])
      },
      test("guard on empty string") {
          assertZIO(AuthController.authorize("").exit)(failsWithA[UnauthorizedErrorResponse])
      },
      test("guard on null string") {
        assertZIO(AuthController.authorize(null).exit)(failsWithA[UnauthorizedErrorResponse])
      }
    )
  }.provide(AuthControllerImpl.layer)
}
