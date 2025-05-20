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

object RawStatusUnitTests extends ConfigProviderSpec {
  private val rawStatusRed: RawStatus = RawStatus.Red("Service is FUBAR", intermittent = false)
  private val rawStatusRedJson = "\"RED(Service is FUBAR)\""
  private val rawStatusRedIntermittent: RawStatus = RawStatus.Red("Service is FUBAR", intermittent = true)
  private val rawStatusRedIntermittentJson = "\"RED[Service is FUBAR]\""
  private val rawStatusAmber: RawStatus = RawStatus.Amber("Service is struggling", intermittent = false)
  private val rawStatusAmberJson = "\"AMBER(Service is struggling)\""
  private val rawStatusAmberIntermittent: RawStatus = RawStatus.Amber("Service is struggling", intermittent = true)
  private val rawStatusAmberIntermittentJson = "\"AMBER[Service is struggling]\""
  private val rawStatusGreen: RawStatus = RawStatus.Green("Service is good")
  private val rawStatusGreenJson = "\"GREEN(Service is good)\""

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ServiceConfigurationSuite")(
      suite("RED")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(rawStatusRed.asJson)
          } yield assert(serialized.toString)(equalTo(rawStatusRedJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(rawStatusRedJson))
            deserialized <- ZIO.fromEither(jsonObject.as[RawStatus])
          } yield assert(deserialized)(equalTo(rawStatusRed))
        }
      ),
      suite("RED INTERMITTENT")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(rawStatusRedIntermittent.asJson)
          } yield assert(serialized.toString)(equalTo(rawStatusRedIntermittentJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(rawStatusRedIntermittentJson))
            deserialized <- ZIO.fromEither(jsonObject.as[RawStatus])
          } yield assert(deserialized)(equalTo(rawStatusRedIntermittent))
        }
      ),
      suite("AMBER")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(rawStatusAmber.asJson)
          } yield assert(serialized.toString)(equalTo(rawStatusAmberJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(rawStatusAmberJson))
            deserialized <- ZIO.fromEither(jsonObject.as[RawStatus])
          } yield assert(deserialized)(equalTo(rawStatusAmber))
        }
      ),
      suite("AMBER INTERMITTENT")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(rawStatusAmberIntermittent.asJson)
          } yield assert(serialized.toString)(equalTo(rawStatusAmberIntermittentJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(rawStatusAmberIntermittentJson))
            deserialized <- ZIO.fromEither(jsonObject.as[RawStatus])
          } yield assert(deserialized)(equalTo(rawStatusAmberIntermittent))
        }
      ),
      suite("GREEN")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(rawStatusGreen.asJson)
          } yield assert(serialized.toString)(equalTo(rawStatusGreenJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(rawStatusGreenJson))
            deserialized <- ZIO.fromEither(jsonObject.as[RawStatus])
          } yield assert(deserialized)(equalTo(rawStatusGreen))
        }
      ),
    )
  }
}
