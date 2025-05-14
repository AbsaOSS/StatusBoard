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
