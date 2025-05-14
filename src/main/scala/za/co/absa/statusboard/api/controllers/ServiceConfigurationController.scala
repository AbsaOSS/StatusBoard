package za.co.absa.statusboard.api.controllers

import za.co.absa.statusboard.model.ApiResponse.{MultiApiResponse, SingleApiResponse}
import za.co.absa.statusboard.model.{ErrorResponse, RawStatus, ServiceConfiguration, ServiceConfigurationReference}
import zio._
import zio.macros.accessible

/**
 *  Interface for managing service configurations
 */
@accessible
trait ServiceConfigurationController {
  /**
   *  Retrieves all service configurations
   *  corresponds to REST GET
   */
  def getServiceConfigurations(includeHidden: Option[Boolean]): IO[ErrorResponse, MultiApiResponse[ServiceConfiguration]]

  /**
   *  Retrieves a specific service configuration by its unique service name.
   *  corresponds to REST GET
   *
   *  @param environment Environment
   *  @param serviceName Service name
   */
  def getServiceConfiguration(environment: String, serviceName: String): IO[ErrorResponse, SingleApiResponse[ServiceConfiguration]]

  /**
   *  Retrieves dependencies of a specific service configuration
   *  corresponds to REST GET
   *
   *  @param environment Environment
   *  @param serviceName Service name
   */
  def getServiceConfigurationDependencies(environment: String, serviceName: String): IO[ErrorResponse, MultiApiResponse[ServiceConfigurationReference]]

  /**
   *  Retrieves dependents of a specific service configuration
   *  corresponds to REST GET
   *
   *  @param environment Environment
   *  @param serviceName Service name
   */
  def getServiceConfigurationDependents(environment: String, serviceName: String): IO[ErrorResponse, MultiApiResponse[ServiceConfigurationReference]]

  /**
   *  Creates a new service configuration
   *  corresponds to REST POST
   *  Fails if service configuration with given name already exists
   *
   *  @param configuration new service configuration
   */
  def createNewServiceConfiguration(configuration: ServiceConfiguration): IO[ErrorResponse, Unit]

  /**
   *  Updates an existing service configuration
   *  corresponds to REST PUT and is IDEMPOTENT
   *  Fails if service configuration with given name does not exist
   *
   *  @param environment: Environment
   *  @param serviceName Service name
   *  @param configuration Modified service configuration
   */
  def updateExistingServiceConfiguration(environment: String, serviceName: String, configuration: ServiceConfiguration): IO[ErrorResponse, Unit]

  /**
   *  Updates an existing service configuration
   *    including name change - also performs rename in history
   *  corresponds to REST POST
   *  Fails if service configuration with given name does not exist
   *
   *  @param environment: Old Environment
   *  @param serviceName Old Service name
   *  @param configuration Modified service configuration (with new name and environment)
   */
  def renameExistingServiceConfiguration(environment: String, serviceName: String, configuration: ServiceConfiguration): IO[ErrorResponse, Unit]

  /**
   *  Updates maintenance message of existing service configuration
   *   - immediately resets monitoring for service configuration
   *
   *  @param environment: Environment
   *  @param serviceName Service name
   *  @param maintenanceMessage Maintenance message to be set
   */
  def setMaintenanceMessage(environment: String, serviceName: String, maintenanceMessage: String): IO[ErrorResponse, Unit]

  /**
   *  Set temporary status (incl maintenance message) for existing service configuration
   *   - immediately resets monitoring for service configuration
   *
   *  @param environment: Environment
   *  @param serviceName Service name
   *  @param maintenanceMessage Maintenance message to be set
   *  @param status Temporary status to be set
   */
  def setTemporaryStatus(environment: String, serviceName: String, status: RawStatus, maintenanceMessage: String): IO[ErrorResponse, Unit]

  /**
   *  Restore from temporary status (incl removing maintenance message) for existing service configuration
   *   - immediately resets monitoring for service configuration
   *
   *  @param environment: Environment
   *  @param serviceName Service name
   */
  def restoreFromTemporaryStatus(environment: String, serviceName: String): IO[ErrorResponse, Unit]

  /**
   *  Deletes a specific service configuration
   *  corresponds to REST DELETE and is IDEMPOTENT (does nothing for non-existing service configuration)
   *
   *  @param environment Environment
   *  @param serviceName Name of service which configuration is to be deleted
   */
  def deleteServiceConfiguration(environment: String, serviceName: String): IO[ErrorResponse, Unit]
}
