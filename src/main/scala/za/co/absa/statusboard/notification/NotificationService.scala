package za.co.absa.statusboard.notification

import za.co.absa.statusboard.model.{NotificationAction, NotificationCondition, RefinedStatus}
import zio._
import zio.macros.accessible

/**
 *  Interface for managing notifications.
 */
@accessible
trait NotificationService {
  /**
   *  Performs notification action when condition is met
   *
   *  @param condition Condition for when notification needs to happen
   *  @param actions Notification actions to happen
   *  @return A [[RefinedStatus]]
   *          when notification happens and is success, amended status (notified=true)
   *          when no notification is required, original status
   *          when partial failure happens
   *            proceeds with as much notifications as it can
   *            logs relevant errors
   *            returns original status (notified=false)
   */
  def notifyIfApplicable(condition: NotificationCondition, actions: Seq[NotificationAction], status: RefinedStatus): UIO[RefinedStatus]
}
