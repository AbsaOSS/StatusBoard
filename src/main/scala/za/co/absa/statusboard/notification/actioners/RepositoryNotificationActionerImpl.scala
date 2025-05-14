package za.co.absa.statusboard.notification.actioners
import za.co.absa.statusboard.model.{NotificationAction, RefinedStatus}
import za.co.absa.statusboard.repository.NotificationRepository
import zio.{Task, ZIO, ZLayer}

class RepositoryNotificationActionerImpl(notificationRepository: NotificationRepository) extends RepositoryNotificationActioner {
  override def notify(action: NotificationAction.Repository, status: RefinedStatus): Task[Unit] = notificationRepository.persistNotification(status)
}

object RepositoryNotificationActionerImpl {
  val layer: ZLayer[NotificationRepository, Throwable, RepositoryNotificationActioner] = ZLayer {
    for {
      notificationRepository <- ZIO.service[NotificationRepository]
    } yield new RepositoryNotificationActionerImpl(notificationRepository)
  }
}
