package za.co.absa.statusboard.notification

import org.mockito.Mockito.{mock, reset, times, verify, when}
import org.mockito.ArgumentMatchers.any
import za.co.absa.statusboard.model.{NotificationAction, NotificationCondition, RefinedStatus}
import za.co.absa.statusboard.notification.actioners.NotificationActioner
import za.co.absa.statusboard.notification.deciders.NotificationDecider
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio.test.Assertion.equalTo
import zio.{Ref, Scope, Task, ZIO, ZLayer}
import zio.test._

import java.time.Duration

object NotificationServiceImplUnitTests extends ConfigProviderSpec {
  private val statusNotNotified = TestData.refinedStatus.copy(notificationSent = false)

  private val notificationConditionYesMock = mock(classOf[NotificationCondition])
  private val notificationConditionNoMock = mock(classOf[NotificationCondition])
  private val notificationDeciderMock = mock(classOf[NotificationDecider])

  private val notificationActionMock = mock(classOf[NotificationAction])
  private val notificationActionFailFastMock = mock(classOf[NotificationAction])
  private val notificationActionTakeTimeMock = mock(classOf[NotificationAction])
  private val notificationActionerMock = mock(classOf[NotificationActioner])

  private def setupMocks(mockCounter: Ref[Int]): Task[Unit] = ZIO.attempt {
    reset(notificationDeciderMock)
    when(notificationDeciderMock.shouldNotify(notificationConditionYesMock, statusNotNotified))
      .thenReturn(ZIO.succeed(true))
    when(notificationDeciderMock.shouldNotify(notificationConditionNoMock, statusNotNotified))
      .thenReturn(ZIO.succeed(false))

    reset(notificationActionerMock)
    when(notificationActionerMock.notify(notificationActionMock, statusNotNotified))
      .thenAnswer(_ => mockCounter.update(_ + 1))
    when(notificationActionerMock.notify(notificationActionFailFastMock, statusNotNotified))
      .thenReturn(ZIO.fail(new Exception("I fail fast")))
    when(notificationActionerMock.notify(notificationActionTakeTimeMock, statusNotNotified))
      .thenAnswer(_ => mockCounter.update(_ + 1).delay(Duration.ofSeconds(2)))
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("NotificationServiceSuite")(
      test("Do not notify when decided not to, return original status") {
        assertZIO(
          for {
            mockCounter <- zio.Ref.make(0)
            _ <- setupMocks(mockCounter)
            afterNotification <- NotificationService.notifyIfApplicable(notificationConditionNoMock, Seq(notificationActionMock), statusNotNotified)
            _ <- ZIO.attempt(verify(notificationDeciderMock, times(1)).shouldNotify(any[NotificationCondition], any[RefinedStatus]))
            _ <- ZIO.attempt(verify(notificationActionerMock, times(0)).notify(any[NotificationAction], any[RefinedStatus]))
            mockCounter_val <- mockCounter.get
            _ <- assertTrue(mockCounter_val == 0)
          } yield afterNotification)(
          equalTo(statusNotNotified)
        )
      },
      test("Do notify when decided yes to, return amended status") {
        assertZIO(
          for {
            mockCounter <- zio.Ref.make(0)
            _ <- setupMocks(mockCounter)
            afterNotification <- NotificationService.notifyIfApplicable(notificationConditionYesMock, Seq(notificationActionMock), statusNotNotified)
            _ <- ZIO.attempt(verify(notificationDeciderMock, times(1)).shouldNotify(any[NotificationCondition], any[RefinedStatus]))
            _ <- ZIO.attempt(verify(notificationActionerMock, times(1)).notify(any[NotificationAction], any[RefinedStatus]))
            _ <- assertZIO(mockCounter.get)(equalTo(1))
          } yield afterNotification)(
          equalTo(statusNotNotified.copy(notificationSent = true))
        )
      },
      test("Do notify all items in sequence, even when there are fails, return original status as notifications were not completed in full") {
        assertZIO(
          for {
            mockCounter <- zio.Ref.make(0)
            _ <- setupMocks(mockCounter)
            afterNotification <- NotificationService.notifyIfApplicable(notificationConditionYesMock,
              Seq(notificationActionMock,
                notificationActionFailFastMock,
                notificationActionTakeTimeMock,
                notificationActionMock,
                notificationActionFailFastMock,
                notificationActionTakeTimeMock
              ),
              statusNotNotified)
            _ <- ZIO.attempt(verify(notificationDeciderMock, times(1)).shouldNotify(any[NotificationCondition], any[RefinedStatus]))
            _ <- ZIO.attempt(verify(notificationActionerMock, times(6)).notify(any[NotificationAction], any[RefinedStatus]))
            _ <- assertZIO(mockCounter.get)(equalTo(4))
          } yield afterNotification)(
          equalTo(statusNotNotified)
        )
      }
    ) @@ TestAspect.sequential @@ TestAspect.withLiveClock
  }.provide(NotificationServiceImpl.layer, ZLayer.succeed(notificationDeciderMock), ZLayer.succeed(notificationActionerMock)
  )
}
