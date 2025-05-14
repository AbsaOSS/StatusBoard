package za.co.absa.statusboard.api.controllers

import za.co.absa.statusboard.model.ErrorResponse
import zio.IO
import zio.macros.accessible

/**
 *  Interface for managing monitoring
 */
@accessible
trait MonitoringController {
  /**
   *  (Re)Starts monitoring for all service configurations
   */
  def restart: IO[ErrorResponse, Unit]

  /**
   *  Restarts monitoring for single service configuration
   *
   *  This method can be used to
   *    start a monitoring task that didn't exist before
   *    update an existing one with changed configuration (including configuration of no monitoring)
   *    restart monitoring task that misbehaves / crashed
   *
   *  @param environment Environment for which the monitoring task is to be restarted.
   *  @param serviceName name of the service for which the monitoring task is to be restarted.
   */
  def restartForService(environment: String, serviceName: String): IO[ErrorResponse, Unit]
}
