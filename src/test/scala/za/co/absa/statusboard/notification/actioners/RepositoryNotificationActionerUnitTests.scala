package za.co.absa.statusboard.notification.actioners

import org.mockito.Mockito._
import za.co.absa.statusboard.model.NotificationAction
import za.co.absa.statusboard.repository.NotificationRepository
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio.test.Assertion.isUnit
import zio.test.{Spec, TestEnvironment, assertZIO}
import zio.{Scope, ZIO, ZLayer}

object RepositoryNotificationActionerUnitTests extends ConfigProviderSpec {
  private val status = TestData.refinedStatus
  private val action = NotificationAction.Repository()
  private val repositoryMock = mock(classOf[NotificationRepository])

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("RepositoryNotificationActionerSuite")(
      test("Pass status directly to repository") {
        assertZIO(
          for {
            _ <- RepositoryNotificationActioner.notify(action, status)
            _ <- ZIO.attempt(verify(repositoryMock, times(1)).persistNotification(status))
          } yield ()
        )(isUnit)
      }
    )
  }.provide(RepositoryNotificationActionerImpl.layer, ZLayer.succeed(repositoryMock))
}
