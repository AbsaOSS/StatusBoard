package za.co.absa.statusboard.model

import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestEnvironment, assert}
import zio.{Scope, ZIO}

object NotificationConditionUnitTests extends ConfigProviderSpec {
  private val durationBased: NotificationCondition = NotificationCondition.DurationBased(5)
  private val durationBasedJson =
    """{
      |  "DurationBased" : {
      |    "secondsInState" : 5
      |  }
      |}""".stripMargin

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ServiceConfigurationSuite")(
      suite("DurationBased")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(durationBased.asJson)
          } yield assert(serialized.toString)(equalTo(durationBasedJson))
        },
        test("JSON deserialization") {
          for {
            jsonObject <- ZIO.fromEither(parse(durationBasedJson))
            deserialized <- ZIO.fromEither(jsonObject.as[NotificationCondition])
          } yield assert(deserialized)(equalTo(durationBased))
        }
      )
    )
  }
}
