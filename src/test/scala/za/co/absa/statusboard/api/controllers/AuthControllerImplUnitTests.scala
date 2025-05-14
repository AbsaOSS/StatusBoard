package za.co.absa.statusboard.api.controllers

import za.co.absa.statusboard.model.ErrorResponse.UnauthorizedErrorResponse
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio._
import zio.test.Assertion.{failsWithA, isUnit}
import zio.test._

object AuthControllerImplUnitTests extends ConfigProviderSpec {
  private val apiKey = "<api-key-value>" // NOTE: this need to correspond to actual value in test config

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("AuthControllerImplSuite")(
      test("pass on correct key") {
          assertZIO(AuthController.authorize(apiKey))(isUnit)
      },
      test("guard on wrong key") {
          assertZIO(AuthController.authorize("SpoofedKey").exit)(failsWithA[UnauthorizedErrorResponse])
      },
      test("guard on empty string") {
          assertZIO(AuthController.authorize("").exit)(failsWithA[UnauthorizedErrorResponse])
      },
      test("guard on null string") {
        assertZIO(AuthController.authorize(null).exit)(failsWithA[UnauthorizedErrorResponse])
      }
    )
  }.provide(AuthControllerImpl.layer)
}
