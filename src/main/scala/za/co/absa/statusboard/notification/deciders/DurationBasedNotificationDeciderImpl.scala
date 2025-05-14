package za.co.absa.statusboard.notification.deciders
import za.co.absa.statusboard.model.RawStatus.Green
import za.co.absa.statusboard.model.{NotificationCondition, RawStatus, RefinedStatus}
import zio.{UIO, ZIO, ZLayer}

import java.time.Duration

object DurationBasedNotificationDeciderImpl extends DurationBasedNotificationDecider {
  override def shouldNotify(condition: NotificationCondition.DurationBased, status: RefinedStatus): UIO[Boolean] = {
    ZIO.succeed {
      !status.notificationSent &&
        isNotGreen(status.status) &&
        (!status.status.intermittent || condition.secondsInState < Duration.between(status.firstSeen, status.lastSeen).toSeconds)
    }
  }

  private def isNotGreen(status: RawStatus): Boolean = status match {
    case Green(_) => false
    case _ => true
  }

  val layer: ZLayer[Any, Throwable, DurationBasedNotificationDecider] = ZLayer.succeed(DurationBasedNotificationDeciderImpl)
}
