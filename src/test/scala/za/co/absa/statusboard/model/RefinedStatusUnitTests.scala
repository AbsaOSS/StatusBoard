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

import java.time.Instant

object RefinedStatusUnitTests extends ConfigProviderSpec {
  private val refinedStatus: RefinedStatus = RefinedStatus(
    serviceName = "Test Service",
    env = "TestEnv",
    status = RawStatus.Green("Service is good"),
    maintenanceMessage = "Testing as usual",
    firstSeen = Instant.parse("1989-05-29T12:00:00Z"),
    lastSeen = Instant.parse("2024-05-17T12:00:00Z"),
    notificationSent = false
  )
  private val refinedStatusJson =
    """{
      |  "serviceName" : "Test Service",
      |  "env" : "TestEnv",
      |  "status" : "GREEN(Service is good)",
      |  "maintenanceMessage" : "Testing as usual",
      |  "firstSeen" : "1989-05-29T12:00:00Z",
      |  "lastSeen" : "2024-05-17T12:00:00Z",
      |  "notificationSent" : false
      |}""".stripMargin

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ServiceConfigurationSuite")(
      test("JSON serialization") {
        for {
          serialized <- ZIO.succeed(refinedStatus.asJson)
        } yield assert(serialized.toString)(equalTo(refinedStatusJson))
      },
      test("JSON deserialization") {
        for {
          jsonObject <- ZIO.fromEither(parse(refinedStatusJson))
          deserialized <- ZIO.fromEither(jsonObject.as[RefinedStatus])
        } yield assert(deserialized)(equalTo(refinedStatus))
      }
    )
  }
}
