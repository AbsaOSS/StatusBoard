package za.co.absa.statusboard.notification.actioners

import za.co.absa.statusboard.model.{NotificationAction, RefinedStatus}
import zio._
import zio.macros.accessible

@accessible
trait NotificationActioner {
  /**
   *  Performs notification action
   *
   *  @param action Notification action to be performed
   *  @param status Refined status to be reported in notification action
   */
  def notify(action: NotificationAction, status: RefinedStatus): Task[Unit]
}
