package za.co.absa.statusboard.notification.deciders

import za.co.absa.statusboard.model.{NotificationCondition, RefinedStatus}
import zio._
import zio.macros.accessible

/**
 * Specialization of NotificationDecider for DurationBased
 */
@accessible
trait DurationBasedNotificationDecider {
  def shouldNotify(condition: NotificationCondition.DurationBased, status: RefinedStatus): UIO[Boolean]
}
