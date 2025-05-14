package za.co.absa.statusboard.checker

import za.co.absa.statusboard.model.StatusCheckAction.FixedStatus
import za.co.absa.statusboard.model.StatusCheckAction.TemporaryFixedStatus
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio._
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestEnvironment, assertZIO}

object TemporaryFixedStatusCheckerImplUnitTests extends ConfigProviderSpec {
  private val checkRequest = TemporaryFixedStatus(TestData.rawStatusGreen, FixedStatus(TestData.rawStatusAmber))

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("TemporaryFixedStatusCheckerImplSuite")(
      test("GREEN on success") {
        assertZIO(TemporaryFixedStatusChecker.checkRawStatus(checkRequest))(equalTo(TestData.rawStatusGreen))
      }
    )
  }.provide(
    TemporaryFixedStatusCheckerImpl.layer,
  )
}
