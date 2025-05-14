package za.co.absa.statusboard.notification.actioners

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import za.co.absa.statusboard.model.{NotificationAction, RefinedStatus}
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio.test.Assertion.isUnit
import zio.test.{Spec, TestAspect, TestEnvironment, assertZIO}
import zio.{Scope, Task, ZIO, ZLayer}

object PolymorphicNotificationActionerUnitTests extends ConfigProviderSpec {
  private val statusMock = mock(classOf[RefinedStatus])
  private val emailNotificationActionMock = mock(classOf[NotificationAction.EMail])
  private val emailNotificationActionerMock = mock(classOf[EmailNotificationActioner])

  private val msTeamsNotificationActionMock = mock(classOf[NotificationAction.MSTeams])
  private val msTeamsNotificationActionerMock = mock(classOf[MsTeamsNotificationActioner])

  private val repositoryNotificationActionMock = mock(classOf[NotificationAction.Repository])
  private val repositoryNotificationActionerMock = mock(classOf[RepositoryNotificationActioner])


  private def setupMocks: Task[Unit] = ZIO.attempt {
    reset(emailNotificationActionerMock)
    reset(msTeamsNotificationActionerMock)
    reset(repositoryNotificationActionerMock)
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("PolymorphicNotificationActionerSuite")(
      test("Pass E-Mail action to email actioner") {
        assertZIO(
          for {
            result <- NotificationActioner.notify(emailNotificationActionMock, statusMock)
            _ <- ZIO.attempt(verify(emailNotificationActionerMock, times(1)).notify(any[NotificationAction.EMail], any[RefinedStatus]))
            _ <- ZIO.attempt(verify(msTeamsNotificationActionerMock, times(0)).notify(any[NotificationAction.MSTeams], any[RefinedStatus]))
            _ <- ZIO.attempt(verify(repositoryNotificationActionerMock, times(0)).notify(any[NotificationAction.Repository], any[RefinedStatus]))
          } yield result
        )(isUnit)
      },
      test("Pass MS Teams action to msTeams actioner") {
        assertZIO(
          for {
            result <- NotificationActioner.notify(msTeamsNotificationActionMock, statusMock)
            _ <- ZIO.attempt(verify(emailNotificationActionerMock, times(0)).notify(any[NotificationAction.EMail], any[RefinedStatus]))
            _ <- ZIO.attempt(verify(msTeamsNotificationActionerMock, times(1)).notify(any[NotificationAction.MSTeams], any[RefinedStatus]))
            _ <- ZIO.attempt(verify(repositoryNotificationActionerMock, times(0)).notify(any[NotificationAction.Repository], any[RefinedStatus]))
          } yield result
        )(isUnit)
      },
      test("Pass Repository action to repository actioner") {
        assertZIO(
          for {
            result <- NotificationActioner.notify(repositoryNotificationActionMock, statusMock)
            _ <- ZIO.attempt(verify(emailNotificationActionerMock, times(0)).notify(any[NotificationAction.EMail], any[RefinedStatus]))
            _ <- ZIO.attempt(verify(msTeamsNotificationActionerMock, times(0)).notify(any[NotificationAction.MSTeams], any[RefinedStatus]))
            _ <- ZIO.attempt(verify(repositoryNotificationActionerMock, times(1)).notify(any[NotificationAction.Repository], any[RefinedStatus]))
          } yield result
        )(isUnit)
      }
    ) @@ TestAspect.before(setupMocks) @@ TestAspect.sequential
  }.provide(PolymorphicNotificationActioner.layer,
    ZLayer.succeed(emailNotificationActionerMock),
    ZLayer.succeed(msTeamsNotificationActionerMock),
    ZLayer.succeed(repositoryNotificationActionerMock)
  )
}
