package za.co.absa.statusboard.monitoring

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.{mock, reset, times, verify, when}
import za.co.absa.statusboard.checker.Checker
import za.co.absa.statusboard.model.AppError.DatabaseError
import za.co.absa.statusboard.model.AppError.DatabaseError.{GeneralDatabaseError, RecordNotFoundDatabaseError}
import za.co.absa.statusboard.model.{NotificationAction, NotificationCondition, RawStatus, RefinedStatus, StatusCheckAction}
import za.co.absa.statusboard.notification.NotificationService
import za.co.absa.statusboard.repository.StatusRepository
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio.{Scope, Task, ZIO, ZLayer}
import zio.test.Assertion.isUnit
import zio.test.{Spec, TestAspect, TestEnvironment, assertZIO}

import scala.math.Ordered.orderingToOrdered

object MonitoringWorkerImplUnitTests extends ConfigProviderSpec {
  private val service = TestData.serviceConfiguration
  private val lastRawStatus = TestData.rawStatusGreen
  private val differentRawStatus = TestData.rawStatusRed
  private val lastStatus = TestData.refinedStatus.copy(status = lastRawStatus)
  private val afterNotificationStatus = lastStatus.copy(notificationSent = true)

  private val repositoryMock = mock(classOf[StatusRepository])
  private val checkerMock = mock(classOf[Checker])
  private val notificationServiceMock = mock(classOf[NotificationService])

  private def setupMocks(lastStatus: RefinedStatus, currentRawStatus: RawStatus, afterNotificationStatus: RefinedStatus): Task[Unit] = ZIO.attempt {
    reset(checkerMock)
    reset(notificationServiceMock)
    reset(repositoryMock)
    when(repositoryMock.getLatestStatus(lastStatus.env, lastStatus.serviceName))
      .thenReturn(ZIO.succeed(lastStatus))
    when(checkerMock.checkRawStatus(any[StatusCheckAction]))
      .thenReturn(ZIO.succeed(currentRawStatus))
    when(notificationServiceMock.notifyIfApplicable(any[NotificationCondition], any[Seq[NotificationAction]], any[RefinedStatus]))
      .thenReturn(ZIO.succeed(afterNotificationStatus))
    when(repositoryMock.createOrUpdate(afterNotificationStatus))
      .thenReturn(ZIO.unit)
  }

  private def setupMocks(lastStatusError: DatabaseError, currentRawStatus: RawStatus, afterNotificationStatus: RefinedStatus): Task[Unit] = ZIO.attempt {
    reset(checkerMock)
    reset(notificationServiceMock)
    reset(repositoryMock)
    when(repositoryMock.getLatestStatus(lastStatus.env, lastStatus.serviceName))
      .thenReturn(ZIO.fail(lastStatusError))
    when(checkerMock.checkRawStatus(any[StatusCheckAction]))
      .thenReturn(ZIO.succeed(currentRawStatus))
    when(notificationServiceMock.notifyIfApplicable(any[NotificationCondition], any[Seq[NotificationAction]], any[RefinedStatus]))
      .thenReturn(ZIO.succeed(afterNotificationStatus))
    when(repositoryMock.createOrUpdate(any[RefinedStatus]))
      .thenReturn(ZIO.unit)
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("MonitoringWorkerImplSuite")(
      test("On ongoing status, prolonged refined status should be passed to notification service") {
        assertZIO(for {
          // Arrange
          _ <- setupMocks(lastStatus, lastRawStatus, afterNotificationStatus)
          // Act
          _ <- MonitoringWorker.performMonitoringWork(service)
          // Assert
          - <- ZIO.attempt {
            verify(notificationServiceMock).notifyIfApplicable(
              any[NotificationCondition],
              any[Seq[NotificationAction]],
              argThat[RefinedStatus](notifiedStatus => {
                notifiedStatus.lastSeen > lastStatus.lastSeen &&
                  notifiedStatus == lastStatus.copy(
                    lastSeen = notifiedStatus.lastSeen
                  )
              }))
          }
        } yield ())(isUnit)
      },
      test("On changed raw status, new refined status should be passed to notification service") {
        assertZIO(for {
          // Arrange
          _ <- setupMocks(lastStatus, differentRawStatus, afterNotificationStatus)
          // Act
          _ <- MonitoringWorker.performMonitoringWork(service)
          // Assert
          - <- ZIO.attempt {
            verify(notificationServiceMock).notifyIfApplicable(
              any[NotificationCondition],
              any[Seq[NotificationAction]],
              argThat[RefinedStatus](notifiedStatus => {
                notifiedStatus.firstSeen > lastStatus.firstSeen &&
                  notifiedStatus.lastSeen > lastStatus.lastSeen &&
                  notifiedStatus == lastStatus.copy(
                    firstSeen = notifiedStatus.firstSeen,
                    lastSeen = notifiedStatus.lastSeen,
                    status = differentRawStatus
                  )
              }))
          }
        } yield ())(isUnit)
      },
      test("On changed maintenance message, new refined status should be passed to notification service") {
        assertZIO(for {
          // Arrange
          _ <- setupMocks(lastStatus, lastRawStatus, afterNotificationStatus)
          // Act
          _ <- MonitoringWorker.performMonitoringWork(service.copy(maintenanceMessage = "Under different maintenance now"))
          // Assert
          - <- ZIO.attempt {
            verify(notificationServiceMock).notifyIfApplicable(
              any[NotificationCondition],
              any[Seq[NotificationAction]],
              argThat[RefinedStatus](notifiedStatus => {
                notifiedStatus.firstSeen > lastStatus.firstSeen &&
                  notifiedStatus.lastSeen > lastStatus.lastSeen &&
                  notifiedStatus == lastStatus.copy(
                    firstSeen = notifiedStatus.firstSeen,
                    lastSeen = notifiedStatus.lastSeen,
                    maintenanceMessage = "Under different maintenance now"
                  )
              }))
          }
        } yield ())(isUnit)
      },
      test("On first seen service, new refined status should be passed to notification service") {
        assertZIO(for {
          // Arrange
          _ <- setupMocks(RecordNotFoundDatabaseError("BAKA"), lastRawStatus, afterNotificationStatus)
          // Act
          _ <- MonitoringWorker.performMonitoringWork(service)
          // Assert
          - <- ZIO.attempt {
            verify(notificationServiceMock).notifyIfApplicable(
              any[NotificationCondition],
              any[Seq[NotificationAction]],
              argThat[RefinedStatus](notifiedStatus => {
                notifiedStatus.firstSeen == notifiedStatus.lastSeen &&
                  notifiedStatus == lastStatus.copy(
                    firstSeen = notifiedStatus.firstSeen,
                    lastSeen = notifiedStatus.lastSeen
                  )
              }))
          }
        } yield ())(isUnit)
      },
      test("result notification service call should be stored to repository") {
        assertZIO(for {
          // Arrange
          _ <- setupMocks(lastStatus, lastRawStatus, afterNotificationStatus)
          // Act
          _ <- MonitoringWorker.performMonitoringWork(service)
          // Assert
          - <- ZIO.attempt {
            verify(repositoryMock, times(1)).createOrUpdate(afterNotificationStatus)
          }
        } yield ())(isUnit)
      },
      test("any error during execution should be silently (apart from being logged) swallowed") {
        assertZIO(for {
          // Arrange
          _ <- setupMocks(GeneralDatabaseError("BAKA"), lastRawStatus, afterNotificationStatus)
          // Act
          _ <- MonitoringWorker.performMonitoringWork(service)
          // Assert
          - <- ZIO.attempt {
            verify(repositoryMock, times(0)).createOrUpdate(any[RefinedStatus])
          }
        } yield ())(isUnit)
      }
    ) @@ TestAspect.sequential @@ TestAspect.withLiveClock
  }.provide(MonitoringWorkerImpl.layer, ZLayer.succeed(repositoryMock), ZLayer.succeed(checkerMock), ZLayer.succeed(notificationServiceMock))
}
