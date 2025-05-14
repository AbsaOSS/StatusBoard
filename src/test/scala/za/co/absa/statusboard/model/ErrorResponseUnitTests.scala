package za.co.absa.statusboard.model

import io.circe.syntax.EncoderOps
import za.co.absa.statusboard.model.ErrorResponse._
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestEnvironment, assert}
import zio.{Scope, ZIO}

object ErrorResponseUnitTests extends ConfigProviderSpec {
  private val badRequestResponse = BadRequestResponse("BAKA")
  private val unauthorizedErrorResponse = UnauthorizedErrorResponse("BAKA")
  private val recordNotFoundErrorResponse = RecordNotFoundErrorResponse("BAKA")
  private val dataConflictErrorResponse = DataConflictErrorResponse("BAKA")
  private val internalServerErrorResponse = InternalServerErrorResponse("BAKA")
  private val commonJson =
    """{
      |  "message" : "BAKA"
      |}""".stripMargin

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ServiceConfigurationSuite")(
      suite("BadRequestResponse")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(badRequestResponse.asJson)
          } yield assert(serialized.toString)(equalTo(commonJson))
        }
      ),
      suite("UnauthorizedErrorResponse")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(unauthorizedErrorResponse.asJson)
          } yield assert(serialized.toString)(equalTo(commonJson))
        }
      ),
      suite("RecordNotFoundErrorResponse")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(recordNotFoundErrorResponse.asJson)
          } yield assert(serialized.toString)(equalTo(commonJson))
        }
      ),
      suite("DataConflictErrorResponse")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(dataConflictErrorResponse.asJson)
          } yield assert(serialized.toString)(equalTo(commonJson))
        }
      ),
      suite("InternalServerErrorResponse")(
        test("JSON serialization") {
          for {
            serialized <- ZIO.succeed(internalServerErrorResponse.asJson)
          } yield assert(serialized.toString)(equalTo(commonJson))
        }
      )
    )
  }
}
