package za.co.absa.statusboard.notification.deciders

import za.co.absa.statusboard.model.{NotificationCondition, RefinedStatus}
import zio.macros.accessible
import zio.{UIO, ZIO, ZLayer}

class PolymorphicNotificationDecider(durationBasedNotificationDecider: DurationBasedNotificationDecider) extends NotificationDecider {
  override def shouldNotify(condition: NotificationCondition, status: RefinedStatus): UIO[Boolean] = {
    condition match {
      case durationBased: NotificationCondition.DurationBased  => durationBasedNotificationDecider.shouldNotify(durationBased, status)
      case _ => ZIO.die(new Exception(s"FATAL: Notification condition ${condition.conditionType} not supported"))
    }
  }
}

object PolymorphicNotificationDecider {
  val layer: ZLayer[DurationBasedNotificationDecider, Throwable, NotificationDecider] = ZLayer {
    for {
      durationBasedNotificationDecider <- ZIO.service[DurationBasedNotificationDecider]
    } yield new PolymorphicNotificationDecider(durationBasedNotificationDecider)
  }
}
