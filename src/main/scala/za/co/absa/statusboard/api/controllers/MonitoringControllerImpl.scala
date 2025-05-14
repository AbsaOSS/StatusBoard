package za.co.absa.statusboard.api.controllers

import za.co.absa.statusboard.model.ErrorResponse
import za.co.absa.statusboard.model.ErrorResponse.InternalServerErrorResponse
import za.co.absa.statusboard.monitoring.MonitoringService
import zio._

class MonitoringControllerImpl(monitoringService: MonitoringService) extends MonitoringController {
  override def restart: IO[ErrorResponse, Unit] =
    monitoringService.restart.mapError(error => InternalServerErrorResponse(error.getMessage))

  override def restartForService(environment: String, serviceName: String): IO[ErrorResponse, Unit] =
    monitoringService.restartForService(environment, serviceName).mapError(error => InternalServerErrorResponse(error.getMessage))
}

object MonitoringControllerImpl {
  val layer: RLayer[MonitoringService, MonitoringController] = ZLayer {
    for {
      monitoringService <- ZIO.service[MonitoringService]
    } yield new MonitoringControllerImpl(monitoringService)
  }
}
