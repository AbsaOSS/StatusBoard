package za.co.absa.statusboard.notification.actioners

import za.co.absa.statusboard.model.{NotificationAction, RefinedStatus}
import zio.Task
import zio.macros.accessible

/**
 * Specialization of NotificationActioner for Repository
 */
@accessible
trait RepositoryNotificationActioner {
  def notify(action: NotificationAction.Repository, status: RefinedStatus): Task[Unit]
}
