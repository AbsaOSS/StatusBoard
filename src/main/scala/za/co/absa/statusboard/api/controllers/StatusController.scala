package za.co.absa.statusboard.api.controllers

import za.co.absa.statusboard.model.ApiResponse.{MultiApiResponse, SingleApiResponse}
import za.co.absa.statusboard.model.{ErrorResponse, RefinedStatus}
import zio.IO
import zio.macros.accessible

/**
 *  Interface for obtaining status reports
 */
@accessible
trait StatusController {
  /**
   *  Retrieves the latest status of a given service.
   *
   *  @param environment Environment
   *  @param serviceName Service name
   */
  def getLatestStatus(environment: String, serviceName: String): IO[ErrorResponse, SingleApiResponse[RefinedStatus]]

  /**
   *  Retrieves full status history for a given service.
   *
   *  @param environment Environment
   *  @param serviceName Service name
   */
  def getAllStatuses(environment: String, serviceName: String): IO[ErrorResponse, MultiApiResponse[RefinedStatus]]

  /**
   *  Retrieves the latest status of all active services.
   */
  def getLatestStatusOfAllActiveConfigurations(): IO[ErrorResponse, MultiApiResponse[RefinedStatus]]
}
