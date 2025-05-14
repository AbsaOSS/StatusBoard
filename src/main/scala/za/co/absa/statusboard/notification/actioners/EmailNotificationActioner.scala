package za.co.absa.statusboard.notification.actioners

import za.co.absa.statusboard.model.{NotificationAction, RefinedStatus}
import zio.Task
import zio.macros.accessible

/**
 * Specialization of NotificationActioner for EMail
 */
@accessible
trait EmailNotificationActioner {
  def notify(action: NotificationAction.EMail, status: RefinedStatus): Task[Unit]
}
