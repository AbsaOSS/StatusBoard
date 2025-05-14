package za.co.absa.statusboard.checker

import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import za.co.absa.statusboard.model.StatusCheckAction.FixedStatus
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio._
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestEnvironment, assert, assertZIO}

object FixedStatusCheckerImplUnitTests extends ConfigProviderSpec {
  private val checkRequest = FixedStatus(TestData.rawStatusGreen)

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("FixedStatusCheckerImplSuite")(
      test("GREEN on success") {
        assertZIO(FixedStatusChecker.checkRawStatus(checkRequest))(equalTo(TestData.rawStatusGreen))
      }
    )
  }.provide(
    FixedStatusCheckerImpl.layer,
  )
}
