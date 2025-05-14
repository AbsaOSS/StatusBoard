package za.co.absa.statusboard.monitoring

import zio._
import zio.macros.accessible

/**
 *  Interface for managing monitoring operations.
 */
@accessible
trait MonitoringService {
  /**
   *  (Re)Starts monitoring for all service configurations
   */
  def restart: Task[Unit]

  /**
   *  Stops all live monitoring tasks
   */
  def stop: Task[Unit]

  /**
   *  Restarts monitoring for single service configuration
   *
   *  This method can be used to
   *    start a monitoring task that didn't exist before
   *    update an existing one with changed configuration (including configuration of no monitoring)
   *    restart monitoring task that misbehaves / crashed
   *
   *  @param environment environment for which the monitoring task is to be restarted.
   *  @param serviceName name of the service for which the monitoring task is to be restarted.
   */
  def restartForService(environment: String, serviceName: String): Task[Unit]
}
