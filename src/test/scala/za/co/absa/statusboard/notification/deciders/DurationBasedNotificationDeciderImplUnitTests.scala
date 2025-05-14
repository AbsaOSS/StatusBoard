package za.co.absa.statusboard.notification.deciders

import za.co.absa.statusboard.model.NotificationCondition
import za.co.absa.statusboard.model.RawStatus.{Green, Red}
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestEnvironment, assertZIO}
import zio.Scope

import java.time.Duration

object DurationBasedNotificationDeciderImplUnitTests extends ConfigProviderSpec {
  private val statusNotSent10MinutesGreen = TestData.refinedStatus.copy(
    status = Green("BAD"),
    notificationSent = false,
    lastSeen = TestData.refinedStatus.firstSeen.plus(Duration.ofMinutes(10))
  )

  private val statusNotSent10Minutes = TestData.refinedStatus.copy(
    status = Red("BAD", intermittent = false),
    notificationSent = false,
    lastSeen = TestData.refinedStatus.firstSeen.plus(Duration.ofMinutes(10))
  )

  private val statusNotSentIntermittent10Minutes = TestData.refinedStatus.copy(
    status = Red("BAD", intermittent = true),
    notificationSent = false,
    lastSeen = TestData.refinedStatus.firstSeen.plus(Duration.ofMinutes(10))
  )

  private val statusYesSent10Minutes = TestData.refinedStatus.copy(
    status = Red("BAD", intermittent = false),
    notificationSent = true,
    lastSeen = TestData.refinedStatus.firstSeen.plus(Duration.ofMinutes(10))
  )

  private val statusYesSentIntermittent10Minutes = TestData.refinedStatus.copy(
    status = Red("BAD", intermittent = true),
    notificationSent = true,
    lastSeen = TestData.refinedStatus.firstSeen.plus(Duration.ofMinutes(10))
  )

  private val conditionDuration5Minutes = NotificationCondition.DurationBased(Duration.ofMinutes(5).toSeconds.toInt)

  private val conditionDuration30Minutes = NotificationCondition.DurationBased(Duration.ofMinutes(30).toSeconds.toInt)

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("DurationBasedNotificationDeciderImplSuite")(
      test("Should not request notification, when already sent regardless of being over time") {
        assertZIO(DurationBasedNotificationDecider.shouldNotify(conditionDuration5Minutes, statusYesSent10Minutes))(
          equalTo(false)
        )
      },
      test("Should not request notification, when already sent (intermittent) regardless of being over time") {
        assertZIO(DurationBasedNotificationDecider.shouldNotify(conditionDuration5Minutes, statusYesSentIntermittent10Minutes))(
          equalTo(false)
        )
      },
      test("Should not request notification, when already sent and not being over time") {
        assertZIO(DurationBasedNotificationDecider.shouldNotify(conditionDuration30Minutes, statusYesSent10Minutes))(
          equalTo(false)
        )
      },
      test("Should not request notification, when already sent (intermittnent) and not being over time") {
        assertZIO(DurationBasedNotificationDecider.shouldNotify(conditionDuration30Minutes, statusYesSentIntermittent10Minutes))(
          equalTo(false)
        )
      },
      test("Should not request notification, when being over time but green #160") {
        assertZIO(DurationBasedNotificationDecider.shouldNotify(conditionDuration5Minutes, statusNotSent10MinutesGreen))(
          equalTo(false)
        )
      },
      test("Should yes request notification, when being over time") {
        assertZIO(DurationBasedNotificationDecider.shouldNotify(conditionDuration5Minutes, statusNotSent10Minutes))(
          equalTo(true)
        )
      },
      test("Should yes request notification, when being over time (intermittent)") {
        assertZIO(DurationBasedNotificationDecider.shouldNotify(conditionDuration5Minutes, statusNotSentIntermittent10Minutes))(
          equalTo(true)
        )
      },
      test("Should yes request notification, when not being over time and NOT intermittent") {
        assertZIO(DurationBasedNotificationDecider.shouldNotify(conditionDuration30Minutes, statusNotSent10Minutes))(
          equalTo(true)
        )
      },
      test("Should not request notification, when not being over time (intermittent)") {
        assertZIO(DurationBasedNotificationDecider.shouldNotify(conditionDuration30Minutes, statusNotSentIntermittent10Minutes))(
          equalTo(false)
        )
      }
    )
  }.provide(DurationBasedNotificationDeciderImpl.layer
  )
}
