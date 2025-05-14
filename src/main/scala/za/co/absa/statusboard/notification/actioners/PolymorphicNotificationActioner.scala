package za.co.absa.statusboard.notification.actioners

import za.co.absa.statusboard.model.{NotificationAction, RefinedStatus}
import zio.{Task, ZIO, ZLayer}

class PolymorphicNotificationActioner(
  repositoryNotificationActioner: RepositoryNotificationActioner,
  emailNotificationActioner: EmailNotificationActioner,
  msTeamsNotificationActioner: MsTeamsNotificationActioner
  ) extends NotificationActioner {
  override def notify(action: NotificationAction, status: RefinedStatus): Task[Unit] = {
    action match {
      case dynamoDB: NotificationAction.Repository  => repositoryNotificationActioner.notify(dynamoDB, status)
      case email: NotificationAction.EMail  => emailNotificationActioner.notify(email, status)
      case msTeams: NotificationAction.MSTeams  => msTeamsNotificationActioner.notify(msTeams, status)
      case _ => ZIO.die(new Exception(s"FATAL: Notification action ${action.actionType} not supported"))
    }
  }
}

object PolymorphicNotificationActioner {
  val layer: ZLayer[
    RepositoryNotificationActioner with EmailNotificationActioner with MsTeamsNotificationActioner,
    Throwable,
    NotificationActioner] = ZLayer {
    for {
      repositoryNotificationActioner <- ZIO.service[RepositoryNotificationActioner]
      emailNotificationActioner <- ZIO.service[EmailNotificationActioner]
      msTeamsNotificationActioner <- ZIO.service[MsTeamsNotificationActioner]
    } yield new PolymorphicNotificationActioner(
      repositoryNotificationActioner,
      emailNotificationActioner,
      msTeamsNotificationActioner)
  }
}
