package za.co.absa.statusboard.providers

import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio._
import zio.config.magnolia.deriveConfig
import zio.test.Assertion.isUnit
import zio.test._

object EmailProviderImplSMTPIntegrationTests extends ConfigProviderSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("EmailProviderImplSuite")(
      test("sending mail should not crash") {
        assertZIO(
          for {
            testTarget <- ZIO.config[String](testTargetConfig)
            _ <- ZIO.serviceWithZIO[EmailProvider](_.sendEmail(Seq(testTarget), "Integration Test", "testing like crazy"))
          } yield ()
        )(isUnit)
      }
    )
  }.provide(EmailProviderImpl.layer)

  private val testTargetConfig = deriveConfig[String].nested("providers", "email", "testTargetAddress")
}
