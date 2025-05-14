package za.co.absa.statusboard.notification.deciders

import za.co.absa.statusboard.model.{NotificationCondition, RefinedStatus}
import zio._
import zio.macros.accessible

@accessible
trait NotificationDecider {
  /**
   *  Decides whether notification should be actioned
   *
   *  @param condition Condition when notification should be actioned
   *  @param status Refined status of the service
   */
  def shouldNotify(condition: NotificationCondition, status: RefinedStatus): UIO[Boolean]
}
