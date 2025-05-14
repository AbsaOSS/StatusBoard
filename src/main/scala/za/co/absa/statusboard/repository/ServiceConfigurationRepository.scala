package za.co.absa.statusboard.repository

import za.co.absa.statusboard.model.AppError.DatabaseError
import za.co.absa.statusboard.model.ServiceConfiguration
import zio._
import zio.macros.accessible

/**
 *  Interface for a service configurations repository.
 */
@accessible
trait ServiceConfigurationRepository {
  /**
   *  Retrieves all service configurations
   *  corresponds to REST GET
   */
  def getServiceConfigurations: IO[DatabaseError, Seq[ServiceConfiguration]]

  /**
   *  Retrieves a specific service configuration by its unique service name.
   *  corresponds to REST GET/<service_name>
   *
   *  @param environment Environment
   *  @param serviceName Service name
   */
  def getServiceConfiguration(environment: String, serviceName: String): IO[DatabaseError, ServiceConfiguration]

  /**
   *  Creates a new service configuration
   *  corresponds to REST POST
   *  Fails if service configuration with given name already exists
   *
   *  @param configuration new service configuration
   */
  def createNewServiceConfiguration(configuration: ServiceConfiguration): IO[DatabaseError, Unit]

  /**
   *  Updates an existing service configuration
   *  corresponds to REST PUT and is IDEMPOTENT
   *  Fails if service configuration with given name does not exist
   *
   *  @param configuration Modified service configuration
   */
  def updateExistingServiceConfiguration(configuration: ServiceConfiguration): IO[DatabaseError, Unit]

  /**
   *  Deletes a specific service configuration
   *  corresponds to REST DELETE and is IDEMPOTENT (does nothing for non-existing service configuration)
   *
   *  @param environment Environment
   *  @param serviceName Name of service which configuration is to be deleted
   */
  def deleteServiceConfiguration(environment: String, serviceName: String): IO[DatabaseError, Unit]
}
