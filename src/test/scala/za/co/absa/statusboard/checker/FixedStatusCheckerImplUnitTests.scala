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

package za.co.absa.statusboard.checker

import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import za.co.absa.statusboard.model.StatusCheckAction.FixedStatus
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio._
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestEnvironment, assert, assertZIO}

object FixedStatusCheckerImplUnitTests extends ConfigProviderSpec {
  private val checkRequest = FixedStatus(TestData.rawStatusGreen)

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("FixedStatusCheckerImplSuite")(
      test("GREEN on success") {
        assertZIO(FixedStatusChecker.checkRawStatus(checkRequest))(equalTo(TestData.rawStatusGreen))
      }
    )
  }.provide(
    FixedStatusCheckerImpl.layer,
  )
}
