package za.co.absa.statusboard.providers

import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio._
import zio.test.Assertion.isUnit
import zio.test._

object MsTeamsProviderMSTeamsIntegrationTests extends ConfigProviderSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("MsTeamsProviderImplSuite")(
      test("sending notification should not crash") {
        assertZIO(ZIO.serviceWithZIO[MsTeamsProvider](_.sendMessage(message)))(isUnit)
      }
    )
  }.provide(MsTeamsProviderImpl.layer, BlazeHttpClientProvider.layer, zio.Scope.default)

  private val message = "{\"text\": \"I am a MS Teams integration test message from status-board\"}"
}
