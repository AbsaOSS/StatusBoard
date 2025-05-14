package za.co.absa.statusboard.notification.deciders

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, times, verify, when}
import za.co.absa.statusboard.model.{NotificationCondition, RefinedStatus}
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio.{Scope, Task, ZIO, ZLayer}
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestAspect, TestEnvironment, assertZIO}

object PolymorphicNotificationDeciderUnitTests extends ConfigProviderSpec {
  private val statusMock = mock(classOf[RefinedStatus])

  private val durationBasedNotificationDeciderMock = mock(classOf[DurationBasedNotificationDecider])

  private val durationBasedNotificationCondition = mock(classOf[NotificationCondition.DurationBased])

  private def setupMocks: Task[Unit] = ZIO.attempt {
    reset(durationBasedNotificationDeciderMock)
    when(durationBasedNotificationDeciderMock.shouldNotify(durationBasedNotificationCondition, statusMock))
      .thenReturn(ZIO.succeed(true))
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("PolymorphicNotificationDeciderSuite")(
      test("Pass duration based condition to duration based decider") {
        assertZIO(
          for {
            result <- NotificationDecider.shouldNotify(durationBasedNotificationCondition, statusMock)
            _ <- ZIO.attempt(verify(durationBasedNotificationDeciderMock, times(1)).shouldNotify(any[NotificationCondition.DurationBased], any[RefinedStatus]))
          } yield result)(
          equalTo(true)
        )
      }
    ) @@ TestAspect.before(setupMocks) @@ TestAspect.sequential
  }.provide(PolymorphicNotificationDecider.layer, ZLayer.succeed(durationBasedNotificationDeciderMock)
  )
}
