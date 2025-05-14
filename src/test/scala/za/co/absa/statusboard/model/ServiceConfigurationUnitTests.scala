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
import za.co.absa.statusboard.model.ServiceConfiguration.ServiceConfigurationLinks
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio.{Scope, ZIO}
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestEnvironment, assert}

object ServiceConfigurationUnitTests extends ConfigProviderSpec {
  private val serviceConfiguration: ServiceConfiguration = ServiceConfiguration(
    name = "Test Service",
    env = "TestEnv",
    hidden = false,
    snowID = "000",
    description = "Testing service",
    maintenanceMessage = "Testing as usual",
    links = ServiceConfigurationLinks(
      home = "http://home.test",
      snow = "http://snow.test",
      support = "http://support.test",
      documentation = "http://docs.test",
      github = "http://git.test"
    ),
    statusCheckAction = StatusCheckAction.HttpGetRequestWithMessage("testMe", "i.dont.exist", 0, "neverExisted"),
    statusCheckIntervalSeconds = 5,
    statusCheckNonGreenIntervalSeconds = 3,
    notificationCondition = NotificationCondition.DurationBased(5),
    notificationAction = Seq(
      NotificationAction.EMail(Seq("mail.no-one.test", "also.no-one.test")),
      NotificationAction.MSTeams(),
      NotificationAction.Repository()
    )
  )
  private val serviceConfigurationJson =
    """{
      |  "name" : "Test Service",
      |  "env" : "TestEnv",
      |  "hidden" : false,
      |  "snowID" : "000",
      |  "description" : "Testing service",
      |  "maintenanceMessage" : "Testing as usual",
      |  "links" : {
      |    "home" : "http://home.test",
      |    "snow" : "http://snow.test",
      |    "support" : "http://support.test",
      |    "documentation" : "http://docs.test",
      |    "github" : "http://git.test"
      |  },
      |  "statusCheckAction" : {
      |    "HttpGetRequestWithMessage" : {
      |      "protocol" : "testMe",
      |      "host" : "i.dont.exist",
      |      "port" : 0,
      |      "path" : "neverExisted"
      |    }
      |  },
      |  "statusCheckIntervalSeconds" : 5,
      |  "statusCheckNonGreenIntervalSeconds" : 3,
      |  "notificationCondition" : {
      |    "DurationBased" : {
      |      "secondsInState" : 5
      |    }
      |  },
      |  "notificationAction" : [
      |    {
      |      "EMail" : {
      |        "addresses" : [
      |          "mail.no-one.test",
      |          "also.no-one.test"
      |        ]
      |      }
      |    },
      |    {
      |      "MSTeams" : {
      |        💩
      |      }
      |    },
      |    {
      |      "Repository" : {
      |        💩
      |      }
      |    }
      |  ]
      |}""".stripMargin.replace("💩", "") // IntelliJ strips trailing spaces even in verbatim string

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ServiceConfigurationSuite")(
      test("JSON serialization") {
        for {
          serialized <- ZIO.succeed(serviceConfiguration.asJson)
        } yield assert(serialized.toString)(equalTo(serviceConfigurationJson))
      },
      test("JSON deserialization") {
        for {
          jsonObject <- ZIO.fromEither(parse(serviceConfigurationJson))
          deserialized <- ZIO.fromEither(jsonObject.as[ServiceConfiguration])
        } yield assert(deserialized)(equalTo(serviceConfiguration))
      }
    )
  }
}
