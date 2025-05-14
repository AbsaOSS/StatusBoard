package za.co.absa.statusboard.model

import io.circe.syntax.EncoderOps
import za.co.absa.statusboard.model.ApiResponse._
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestEnvironment, assert}
import zio.{Scope, ZIO}

object ApiResponseUnitTests extends ConfigProviderSpec {
  private val singleApiResponse = SingleApiResponse("FOO")
  private val singleApiResponseJson =
    """{
      |  "record" : "FOO"
      |}""".stripMargin

  private val multiApiResponse = MultiApiResponse(Seq("FOO", "BAR", "FOOBAR"))
  private val multiApiResponseJson =
    """{
      |  "records" : [
      |    "FOO",
      |    "BAR",
      |    "FOOBAR"
      |  ]
      |}""".stripMargin

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ServiceConfigurationSuite")(
      suite("SingleApiResponse")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(singleApiResponse.asJson)
          } yield assert(serialized.toString)(equalTo(singleApiResponseJson))
        }
      ),
      suite("MultiApiResponse")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(multiApiResponse.asJson)
          } yield assert(serialized.toString)(equalTo(multiApiResponseJson))
        }
      )
    )
  }
}
