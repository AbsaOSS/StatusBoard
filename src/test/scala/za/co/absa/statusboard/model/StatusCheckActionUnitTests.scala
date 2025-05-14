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
import software.amazon.awssdk.regions.Region
import za.co.absa.statusboard.model.StatusCheckAction.CompositionItem.{Direct, Partial}
import za.co.absa.statusboard.model.StatusCheckAction._
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio._
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestEnvironment, assert}

object StatusCheckActionUnitTests extends ConfigProviderSpec {
  private val httpGetRequestStatusCodeOnly: StatusCheckAction= HttpGetRequestStatusCodeOnly(protocol = "TEST", host = "test", port = 0, path = "testing")
  private val httpGetRequestStatusCodeOnlyJson =
    """{
      |  "HttpGetRequestStatusCodeOnly" : {
      |    "protocol" : "TEST",
      |    "host" : "test",
      |    "port" : 0,
      |    "path" : "testing"
      |  }
      |}""".stripMargin

  private val httpGetRequestWithMessage: StatusCheckAction = HttpGetRequestWithMessage(protocol = "TEST", host = "test", port = 0, path = "testing")
  private val httpGetRequestWithMessageJson =
    """{
      |  "HttpGetRequestWithMessage" : {
      |    "protocol" : "TEST",
      |    "host" : "test",
      |    "port" : 0,
      |    "path" : "testing"
      |  }
      |}""".stripMargin

  private val httpGetRequestWithJsonStatus: StatusCheckAction = HttpGetRequestWithJsonStatus(
    protocol = "TEST",
    host = "test",
    port = 0,
    path = "testing",
    jsonStatusPath = Seq("status"),
    greenRegex = "GREEN",
    amberRegex = "AMBER")
  private val httpGetRequestWithJsonStatusJson =
    """{
      |  "HttpGetRequestWithJsonStatus" : {
      |    "protocol" : "TEST",
      |    "host" : "test",
      |    "port" : 0,
      |    "path" : "testing",
      |    "jsonStatusPath" : [
      |      "status"
      |    ],
      |    "greenRegex" : "GREEN",
      |    "amberRegex" : "AMBER"
      |  }
      |}""".stripMargin

  private val fixedStatus: StatusCheckAction = FixedStatus(RawStatus.Green("Service is good"))
  private val fixedStatusJson =
    """{
      |  "FixedStatus" : {
      |    "status" : "GREEN(Service is good)"
      |  }
      |}""".stripMargin

  private val temporaryFixedStatus: StatusCheckAction = TemporaryFixedStatus(RawStatus.Amber("Service is not so well", false), FixedStatus(RawStatus.Green("Service is good")))
  private val temporaryFixedStatusJson =
    """{
      |  "TemporaryFixedStatus" : {
      |    "status" : "AMBER(Service is not so well)",
      |    "originalCheck" : {
      |      "FixedStatus" : {
      |        "status" : "GREEN(Service is good)"
      |      }
      |    }
      |  }
      |}""".stripMargin

  private val composition: StatusCheckAction = Composition(
    greenRequiredForGreen = Seq(
      Direct(ServiceConfigurationReference("Test1", "Item1")),
      Partial(
        Seq(
          Direct(ServiceConfigurationReference("Test1.1", "Item1.1")),
          Direct(ServiceConfigurationReference("Test1.2", "Item1.2")),
          Direct(ServiceConfigurationReference("Test1.3", "Item1.3"))
        ),
        2
      )
    ),
    amberRequiredForGreen = Seq(
      Direct(ServiceConfigurationReference("Test2", "Item2")),
      Partial(
        Seq(
          Direct(ServiceConfigurationReference("Test2.1", "Item2.1")),
          Direct(ServiceConfigurationReference("Test2.2", "Item2.2")),
          Direct(ServiceConfigurationReference("Test2.3", "Item2.3"))
        ),
        2
      )
    ),
    greenRequiredForAmber = Seq(
      Direct(ServiceConfigurationReference("Test3", "Item3")),
      Partial(
        Seq(
          Direct(ServiceConfigurationReference("Test3.1", "Item3.1")),
          Direct(ServiceConfigurationReference("Test3.2", "Item3.2")),
          Direct(ServiceConfigurationReference("Test3.3", "Item3.3"))
        ),
        2
      )
    ),
    amberRequiredForAmber = Seq(
      Direct(ServiceConfigurationReference("Test4", "Item4")),
      Partial(
        Seq(
          Direct(ServiceConfigurationReference("Test4.1", "Item4.1")),
          Direct(ServiceConfigurationReference("Test4.2", "Item4.2")),
          Direct(ServiceConfigurationReference("Test4.3", "Item4.3"))
        ),
        2
      )
    )
  )
  private val compositionJson =
    """{
      |  "Composition" : {
      |    "greenRequiredForGreen" : [
      |      {
      |        "environment" : "Test1",
      |        "service" : "Item1"
      |      },
      |      {
      |        "items" : [
      |          {
      |            "environment" : "Test1.1",
      |            "service" : "Item1.1"
      |          },
      |          {
      |            "environment" : "Test1.2",
      |            "service" : "Item1.2"
      |          },
      |          {
      |            "environment" : "Test1.3",
      |            "service" : "Item1.3"
      |          }
      |        ],
      |        "min" : 2
      |      }
      |    ],
      |    "amberRequiredForGreen" : [
      |      {
      |        "environment" : "Test2",
      |        "service" : "Item2"
      |      },
      |      {
      |        "items" : [
      |          {
      |            "environment" : "Test2.1",
      |            "service" : "Item2.1"
      |          },
      |          {
      |            "environment" : "Test2.2",
      |            "service" : "Item2.2"
      |          },
      |          {
      |            "environment" : "Test2.3",
      |            "service" : "Item2.3"
      |          }
      |        ],
      |        "min" : 2
      |      }
      |    ],
      |    "greenRequiredForAmber" : [
      |      {
      |        "environment" : "Test3",
      |        "service" : "Item3"
      |      },
      |      {
      |        "items" : [
      |          {
      |            "environment" : "Test3.1",
      |            "service" : "Item3.1"
      |          },
      |          {
      |            "environment" : "Test3.2",
      |            "service" : "Item3.2"
      |          },
      |          {
      |            "environment" : "Test3.3",
      |            "service" : "Item3.3"
      |          }
      |        ],
      |        "min" : 2
      |      }
      |    ],
      |    "amberRequiredForAmber" : [
      |      {
      |        "environment" : "Test4",
      |        "service" : "Item4"
      |      },
      |      {
      |        "items" : [
      |          {
      |            "environment" : "Test4.1",
      |            "service" : "Item4.1"
      |          },
      |          {
      |            "environment" : "Test4.2",
      |            "service" : "Item4.2"
      |          },
      |          {
      |            "environment" : "Test4.3",
      |            "service" : "Item4.3"
      |          }
      |        ],
      |        "min" : 2
      |      }
      |    ]
      |  }
      |}""".stripMargin

  private val awsRds: StatusCheckAction = AwsRds(region = Region.AWS_GLOBAL.toString, name = "TestInstance")
  private val awsRdsJson =
    """{
      |  "AwsRds" : {
      |    "region" : "aws-global",
      |    "name" : "TestInstance"
      |  }
      |}""".stripMargin

  private val awsRdsCluster: StatusCheckAction = AwsRdsCluster(region = Region.AWS_GLOBAL.toString, name = "TestInstance")
  private val awsRdsClusterJson =
    """{
      |  "AwsRdsCluster" : {
      |    "region" : "aws-global",
      |    "name" : "TestInstance"
      |  }
      |}""".stripMargin

  private val awsEmr: StatusCheckAction = AwsEmr(region = Region.AWS_GLOBAL.toString, clusterId = "TestInstance", checkNodes = false)
  private val awsEmrJson =
    """{
      |  "AwsEmr" : {
      |    "region" : "aws-global",
      |    "clusterId" : "TestInstance",
      |    "checkNodes" : false
      |  }
      |}""".stripMargin

  private val awsEc2AmiCompliance: StatusCheckAction = AwsEc2AmiCompliance(region = Region.AWS_GLOBAL.toString, id = "i-ec2-12345")
  private val awsEc2AmiComplianceJson =
    """{
      |  "AwsEc2AmiCompliance" : {
      |    "region" : "aws-global",
      |    "id" : "i-ec2-12345"
      |  }
      |}""".stripMargin

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("StatusCheckActionSuite")(
      suite("HttpGetRequestStatusCodeOnly")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(httpGetRequestStatusCodeOnly.asJson)
          } yield assert(serialized.toString)(equalTo(httpGetRequestStatusCodeOnlyJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(httpGetRequestStatusCodeOnlyJson))
            deserialized <- ZIO.fromEither(jsonObject.as[StatusCheckAction])
          } yield assert(deserialized)(equalTo(httpGetRequestStatusCodeOnly))
        }
      ),
      suite("HttpGetRequestWithMessage")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(httpGetRequestWithMessage.asJson)
          } yield assert(serialized.toString)(equalTo(httpGetRequestWithMessageJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(httpGetRequestWithMessageJson))
            deserialized <- ZIO.fromEither(jsonObject.as[StatusCheckAction])
          } yield assert(deserialized)(equalTo(httpGetRequestWithMessage))
        }
      ),
      suite("HttpGetRequestWithJsonStatus")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(httpGetRequestWithJsonStatus.asJson)
          } yield assert(serialized.toString)(equalTo(httpGetRequestWithJsonStatusJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(httpGetRequestWithJsonStatusJson))
            deserialized <- ZIO.fromEither(jsonObject.as[StatusCheckAction])
          } yield assert(deserialized)(equalTo(httpGetRequestWithJsonStatus))
        }
      ),
      suite("Composition")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(composition.asJson)
          } yield assert(serialized.toString)(equalTo(compositionJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(compositionJson))
            deserialized <- ZIO.fromEither(jsonObject.as[StatusCheckAction])
          } yield assert(deserialized)(equalTo(composition))
        }
      ),
      suite("AwsRds")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(awsRds.asJson)
          } yield assert(serialized.toString)(equalTo(awsRdsJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(awsRdsJson))
            deserialized <- ZIO.fromEither(jsonObject.as[StatusCheckAction])
          } yield assert(deserialized)(equalTo(awsRds))
        }
      ),
      suite("AwsRdsCluster")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(awsRdsCluster.asJson)
          } yield assert(serialized.toString)(equalTo(awsRdsClusterJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(awsRdsClusterJson))
            deserialized <- ZIO.fromEither(jsonObject.as[StatusCheckAction])
          } yield assert(deserialized)(equalTo(awsRdsCluster))
        }
      ),
      suite("AwsEmr")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(awsEmr.asJson)
          } yield assert(serialized.toString)(equalTo(awsEmrJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(awsEmrJson))
            deserialized <- ZIO.fromEither(jsonObject.as[StatusCheckAction])
          } yield assert(deserialized)(equalTo(awsEmr))
        }
      ),
      suite("AwsEc2AmiCompliance")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(awsEc2AmiCompliance.asJson)
          } yield assert(serialized.toString)(equalTo(awsEc2AmiComplianceJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(awsEc2AmiComplianceJson))
            deserialized <- ZIO.fromEither(jsonObject.as[StatusCheckAction])
          } yield assert(deserialized)(equalTo(awsEc2AmiCompliance))
        }
      ),
      suite("FixedStatus")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(fixedStatus.asJson)
          } yield assert(serialized.toString)(equalTo(fixedStatusJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(fixedStatusJson))
            deserialized <- ZIO.fromEither(jsonObject.as[StatusCheckAction])
          } yield assert(deserialized)(equalTo(fixedStatus))
        }
      ),
      suite("TemporaryFixedStatus")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(temporaryFixedStatus.asJson)
          } yield assert(serialized.toString)(equalTo(temporaryFixedStatusJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(temporaryFixedStatusJson))
            deserialized <- ZIO.fromEither(jsonObject.as[StatusCheckAction])
          } yield assert(deserialized)(equalTo(temporaryFixedStatus))
        }
      )
    )
  }
}
