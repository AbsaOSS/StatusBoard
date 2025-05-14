package za.co.absa.statusboard.api.controllers

import za.co.absa.statusboard.model.ApiResponse.{MultiApiResponse, SingleApiResponse}
import za.co.absa.statusboard.model.AppError.DatabaseError.RecordNotFoundDatabaseError
import za.co.absa.statusboard.model.{ErrorResponse, RefinedStatus}
import za.co.absa.statusboard.model.ErrorResponse.{InternalServerErrorResponse, RecordNotFoundErrorResponse}
import za.co.absa.statusboard.repository.StatusRepository
import zio.{IO, RLayer, ZIO, ZLayer}

class StatusControllerImpl(statusRepository: StatusRepository) extends StatusController {
  override def getLatestStatus(environment: String, serviceName: String): IO[ErrorResponse, SingleApiResponse[RefinedStatus]] =
    statusRepository.getLatestStatus(environment, serviceName)
      .mapBoth(
        {
          case RecordNotFoundDatabaseError(message) => RecordNotFoundErrorResponse(message)
          case otherError => InternalServerErrorResponse(otherError.getMessage)
        },
        SingleApiResponse(_)
      )

  override def getAllStatuses(environment: String, serviceName: String): IO[ErrorResponse, MultiApiResponse[RefinedStatus]] =
    statusRepository.getAllStatuses(environment, serviceName)
      .mapBoth(error => InternalServerErrorResponse(error.getMessage), MultiApiResponse(_))

  override def getLatestStatusOfAllActiveConfigurations(): IO[ErrorResponse, MultiApiResponse[RefinedStatus]] =
    statusRepository.getLatestStatusOfAllActiveConfigurations().mapBoth(error => InternalServerErrorResponse(error.getMessage), data => MultiApiResponse(data.toSeq))
}

object StatusControllerImpl {
  val layer: RLayer[StatusRepository, StatusController] = ZLayer {
    for {
      statusRepository <- ZIO.service[StatusRepository]
    } yield new StatusControllerImpl(statusRepository)
  }
}
