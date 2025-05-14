package za.co.absa.statusboard.notification.actioners

import za.co.absa.statusboard.model.{NotificationAction, RefinedStatus}
import zio.Task
import zio.macros.accessible

/**
 * Specialization of NotificationActioner for MSTeams
 */
@accessible
trait MsTeamsNotificationActioner {
  def notify(action: NotificationAction.MSTeams, status: RefinedStatus): Task[Unit]
}
