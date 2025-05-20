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

package za.co.absa.statusboard.providers

import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio._
import zio.test.Assertion.isUnit
import zio.test._

object MsTeamsProviderMSTeamsIntegrationTests extends ConfigProviderSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("MsTeamsProviderImplSuite")(
      test("sending notification should not crash") {
        assertZIO(ZIO.serviceWithZIO[MsTeamsProvider](_.sendMessage(message)))(isUnit)
      }
    )
  }.provide(MsTeamsProviderImpl.layer, BlazeHttpClientProvider.layer, zio.Scope.default)

  private val message = "{\"text\": \"I am a MS Teams integration test message from status-board\"}"
}
