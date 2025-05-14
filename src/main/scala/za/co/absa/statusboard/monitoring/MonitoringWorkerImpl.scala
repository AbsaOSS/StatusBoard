package za.co.absa.statusboard.monitoring
import za.co.absa.statusboard.checker.Checker
import za.co.absa.statusboard.model.AppError.DatabaseError.RecordNotFoundDatabaseError
import za.co.absa.statusboard.model.RawStatus.Green
import za.co.absa.statusboard.model.{RefinedStatus, ServiceConfiguration}
import za.co.absa.statusboard.notification.NotificationService
import za.co.absa.statusboard.repository.StatusRepository
import zio.{Duration, UIO, ZIO, ZLayer}

import java.time.Instant

class MonitoringWorkerImpl(
  statusRepository: StatusRepository,
  checker: Checker,
  notificationService: NotificationService
) extends MonitoringWorker {

  override def performMonitoringWork(serviceConfiguration: ServiceConfiguration): UIO[Unit] = {
    for {
      _ <- ZIO.logInfo(s"Monitoring iteration for ${serviceConfiguration.env} / ${serviceConfiguration.name}")
      maybeLastStatus <- statusRepository.getLatestStatus(serviceConfiguration.env, serviceConfiguration.name).foldZIO(
        {
          case RecordNotFoundDatabaseError(_) => ZIO.none
          case error => ZIO.fail(error)
        },
        status => ZIO.some(status)
      )
      currentRawStatus <- checker.checkRawStatus(serviceConfiguration.statusCheckAction)
      instantNow <- ZIO.attempt(Instant.now)
      currentStatus <- maybeLastStatus match {
        case Some(ongoingStatus@RefinedStatus(_, _, lastRawStatus, serviceConfiguration.maintenanceMessage, _, _, _))
          if lastRawStatus == currentRawStatus =>
          ZIO.succeed(ongoingStatus.copy(lastSeen = instantNow))
        case _ => ZIO.succeed(
          RefinedStatus(
            serviceName = serviceConfiguration.name,
            env = serviceConfiguration.env,
            status = currentRawStatus,
            maintenanceMessage = serviceConfiguration.maintenanceMessage,
            firstSeen = instantNow,
            lastSeen = instantNow,
            notificationSent = false)
        )
      }
      afterNotificationStatus <- notificationService.notifyIfApplicable(
        serviceConfiguration.notificationCondition,
        serviceConfiguration.notificationAction,
        currentStatus)
      _ <- statusRepository.createOrUpdate(afterNotificationStatus)
      _ <- ZIO.logInfo(s"Monitoring iteration for ${serviceConfiguration.env} / ${serviceConfiguration.name} completed: ${afterNotificationStatus.status}")
      _ <- afterNotificationStatus.status match {
        case Green(_) => ZIO.sleep(Duration.fromSeconds(serviceConfiguration.statusCheckIntervalSeconds))
        case _ => ZIO.sleep(Duration.fromSeconds(serviceConfiguration.statusCheckNonGreenIntervalSeconds))
      }
    } yield ()
  }.catchAll {error =>
    // status check itself can't fail (resolves as RED),
    // notificationService can't fail, solves failure on its end
    // so this failure is either reading/writing to repository or error in the code
    // meaning, not much we can do, apart from logging it and surviving for next round
    // Also don't forget the delay, so we wont loop on error unnecessarily fast
    ZIO.logError(s"An error occurred while monitoring work for ${serviceConfiguration.env} ${serviceConfiguration.name}: ${error.getMessage}") *>
      ZIO.sleep(Duration.fromSeconds(serviceConfiguration.statusCheckIntervalSeconds))
  }
}

object MonitoringWorkerImpl {
  val layer: ZLayer[StatusRepository with Checker with NotificationService, Throwable, MonitoringWorker] = ZLayer {
    for {
      statusRepository <- ZIO.service[StatusRepository]
      checker <- ZIO.service[Checker]
      notificationService <- ZIO.service[NotificationService]
    } yield new MonitoringWorkerImpl(statusRepository, checker, notificationService)
  }
}
