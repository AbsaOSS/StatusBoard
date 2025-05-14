package za.co.absa.statusboard.notification
import za.co.absa.statusboard.model.{NotificationAction, NotificationCondition, RefinedStatus}
import za.co.absa.statusboard.notification.actioners.NotificationActioner
import za.co.absa.statusboard.notification.deciders.NotificationDecider
import zio.{UIO, ZIO, ZLayer}

class NotificationServiceImpl(notificationDecider: NotificationDecider, notificationActioner: NotificationActioner) extends NotificationService {
  override def notifyIfApplicable(condition: NotificationCondition, actions: Seq[NotificationAction], status: RefinedStatus): UIO[RefinedStatus] = {
    for {
      shouldNotify <- notificationDecider.shouldNotify(condition, status)
      afterPotentialNotify <- if (!shouldNotify) ZIO.succeed(status) else for {
        notificationSuccesses <- ZIO.foreach(actions)(action =>
          notificationActioner.notify(action, status).foldZIO(
            err => ZIO.logError(s"Failed to send notification [$action] for $status with ${err.getMessage}").as(false),
            _ => ZIO.succeed(true)))
      } yield if (notificationSuccesses.forall(item => item)) status.copy(notificationSent = true) else status
    } yield afterPotentialNotify
  }
}

object NotificationServiceImpl {
  val layer: ZLayer[NotificationDecider with NotificationActioner, Throwable, NotificationService] = ZLayer {
    for {
      notificationDecider <- ZIO.service[NotificationDecider]
      notificationActioner <- ZIO.service[NotificationActioner]
    } yield new NotificationServiceImpl(notificationDecider, notificationActioner)
  }
}
