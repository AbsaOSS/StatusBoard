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
import zio.config.magnolia.deriveConfig
import zio.test.Assertion.isUnit
import zio.test._

object EmailProviderImplSMTPIntegrationTests extends ConfigProviderSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("EmailProviderImplSuite")(
      test("sending mail should not crash") {
        assertZIO(
          for {
            testTarget <- ZIO.config[String](testTargetConfig)
            _ <- ZIO.serviceWithZIO[EmailProvider](_.sendEmail(Seq(testTarget), "Integration Test", "testing like crazy"))
          } yield ()
        )(isUnit)
      }
    )
  }.provide(EmailProviderImpl.layer)

  private val testTargetConfig = deriveConfig[String].nested("providers", "email", "testTargetAddress")
}
