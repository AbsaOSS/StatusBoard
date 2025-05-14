package za.co.absa.statusboard.api.controllers

import za.co.absa.statusboard.api.controllers.ServiceConfigurationControllerImpl.{extractDependencies, mapDatabaseErrorToResponseError}
import za.co.absa.statusboard.model.ApiResponse.{MultiApiResponse, SingleApiResponse}
import za.co.absa.statusboard.model.AppError.DatabaseError
import za.co.absa.statusboard.model.ErrorResponse._
import za.co.absa.statusboard.model.{ErrorResponse, NotificationAction, RawStatus, RefinedStatus, ServiceConfiguration, ServiceConfigurationReference, StatusCheckAction}
import za.co.absa.statusboard.model.AppError.DatabaseError.{DataConflictDatabaseError, GeneralDatabaseError, RecordNotFoundDatabaseError}
import za.co.absa.statusboard.model.RawStatus.Black
import za.co.absa.statusboard.model.StatusCheckAction.{Composition, CompositionItem, TemporaryFixedStatus}
import za.co.absa.statusboard.repository.{NotificationRepository, ServiceConfigurationRepository, StatusRepository}
import zio._

import java.time.Instant

class ServiceConfigurationControllerImpl(
  serviceConfigurationRepository: ServiceConfigurationRepository,
  statusRepository: StatusRepository,
  notificationRepository: NotificationRepository,
  monitoringController: MonitoringController
) extends ServiceConfigurationController {
  override def getServiceConfigurations(includeHidden: Option[Boolean]): IO[ErrorResponse, MultiApiResponse[ServiceConfiguration]] = {
    for {
      allConfigurations <- serviceConfigurationRepository.getServiceConfigurations
      filteredConfigurations <- includeHidden match {
        case Some(false) => ZIO.succeed(allConfigurations.filter(!_.hidden))
        case _ => ZIO.succeed(allConfigurations)
      }
    } yield filteredConfigurations
  }.mapBoth(mapDatabaseErrorToResponseError, MultiApiResponse(_))

  override def getServiceConfiguration(environment: String, serviceName: String): IO[ErrorResponse, SingleApiResponse[ServiceConfiguration]] =
    serviceConfigurationRepository.getServiceConfiguration(environment, serviceName).mapBoth(mapDatabaseErrorToResponseError, SingleApiResponse(_))

  override def getServiceConfigurationDependencies(environment: String, serviceName: String): IO[ErrorResponse, MultiApiResponse[ServiceConfigurationReference]] = {
    for {
      configuration <- getServiceConfiguration(environment, serviceName)
    } yield configuration.record.statusCheckAction match {
      case Composition(greenRequiredForGreen, amberRequiredForGreen, greenRequiredForAmber, amberRequiredForAmber) => (
        greenRequiredForGreen.flatMap(extractDependencies) ++
          amberRequiredForGreen.flatMap(extractDependencies) ++
          greenRequiredForAmber.flatMap(extractDependencies) ++
          amberRequiredForAmber.flatMap(extractDependencies)
        ).distinct
      case _ => Seq.empty[ServiceConfigurationReference]
    }
  }.map(MultiApiResponse(_))

  override def getServiceConfigurationDependents(environment: String, serviceName: String): IO[ErrorResponse, MultiApiResponse[ServiceConfigurationReference]] = {
    for {
      configurationReference <- ZIO.succeed(ServiceConfigurationReference(environment, serviceName))
      candidateConfigurations <- getServiceConfigurations(Some(true))
      dependents <- ZIO.foreach(candidateConfigurations.records) { candidateConfiguration =>
        getServiceConfigurationDependencies(candidateConfiguration.env, candidateConfiguration.name)
          .map { candidateDependencies =>
            if (candidateDependencies.records.contains(configurationReference))
              Some(ServiceConfigurationReference(candidateConfiguration.env, candidateConfiguration.name))
            else
              None
          }
      }
    } yield dependents.collect {
      case Some(value) => value
    }
  }.map(MultiApiResponse(_))

  override def createNewServiceConfiguration(configuration: ServiceConfiguration): IO[ErrorResponse, Unit] = {
    if (!hasRepositoryNotificationAction(configuration))
      ZIO.fail(ErrorResponse.DataConflictErrorResponse("Repository notification action is mandatory"))
    else {
        serviceConfigurationRepository.createNewServiceConfiguration(configuration).mapError(mapDatabaseErrorToResponseError) <*
          ZIO.logInfo(s"Created new configuration ${configuration.env} ${configuration.name}")
    }
  }

  override def updateExistingServiceConfiguration(environment: String, serviceName: String, configuration: ServiceConfiguration): IO[ErrorResponse, Unit] = {
    if (serviceName != configuration.name)
      ZIO.fail(ErrorResponse.DataConflictErrorResponse("Direct renaming of service configuration is not supported"))
    else if (environment != configuration.env)
      ZIO.fail(ErrorResponse.DataConflictErrorResponse("Direct replacing of service configuration environment is not supported"))
    else if (!hasRepositoryNotificationAction(configuration))
      ZIO.fail(ErrorResponse.DataConflictErrorResponse("Repository notification action is mandatory"))
    else {
      serviceConfigurationRepository.updateExistingServiceConfiguration(configuration).mapError(mapDatabaseErrorToResponseError) <*
        ZIO.logInfo(s"Updated existing configuration ${configuration.env} ${configuration.name}")
    }
  }

  override def renameExistingServiceConfiguration(environment: String, serviceName: String, configuration: ServiceConfiguration): IO[ErrorResponse, Unit] = {
    // This is heavy-operation, to avoid overly complicated code, certain guarantees are omitted until they become necessary
    // Not thread safe - by design
    //    multiple actors performing racing rename operation, are expected to be non-conflicting
    // Not transactional - by design
    //    After initial checks, one operation starts, it is expected to finish successfully
    //    on failure, admin intervention on DB level might be required
    if (serviceName == configuration.name && environment == configuration.env) {
      ZIO.fail(DataConflictErrorResponse("Renaming not detected, use update configuration instead"))
    } else if (!hasRepositoryNotificationAction(configuration))
      ZIO.fail(DataConflictErrorResponse("Repository notification action is mandatory"))
    else for {
      // Fail rename operation on non-existent service
      _ <- statusRepository.getLatestStatus(environment, serviceName).mapError(mapDatabaseErrorToResponseError)
      // Fail rename operation on conflicting target name
      _ <- (statusRepository.getLatestStatus(configuration.env, configuration.name) *> ZIO.fail(DataConflictDatabaseError(s"Target configuration ${configuration.env} ${configuration.name} already exists ")))
        .catchAll {
          case RecordNotFoundDatabaseError(_) => ZIO.unit
          case err => ZIO.fail(mapDatabaseErrorToResponseError(err))
        }
      // First => remove configuration so its monitoring can't be started anymore
      _ <- serviceConfigurationRepository.deleteServiceConfiguration(environment, serviceName).mapError(mapDatabaseErrorToResponseError)
      _ <- ZIO.logInfo(s"Rename existing configuration $environment $serviceName to ${configuration.env} ${configuration.name}: PENDING 1/6)")
      // <<<-- FAILURE AFTER THIS POINT RESULTS IN CORRUPTED/INCONSISTENT STATE (PARTIAL TRANSACTION DONE) IN DB REQUIRING ADMIN INTERVENTION -->>>
      // Second => stop monitoring, so its state is not updated anymore
      _ <- monitoringController.restartForService(environment, serviceName)
      _ <- ZIO.logInfo(s"Rename existing configuration $environment $serviceName to ${configuration.env} ${configuration.name}: PENDING 2/6)")
      // Third => rename status history
      _ <- statusRepository.renameService(environment, serviceName, configuration.env, configuration.name).mapError(mapDatabaseErrorToResponseError)
      _ <- ZIO.logInfo(s"Rename existing configuration $environment $serviceName to ${configuration.env} ${configuration.name}: PENDING 3/6)")
      // Fourth => rename notification history
      _ <- notificationRepository.renameService(environment, serviceName, configuration.env, configuration.name).mapError(mapDatabaseErrorToResponseError)
      _ <- ZIO.logInfo(s"Rename existing configuration $environment $serviceName to ${configuration.env} ${configuration.name}: PENDING 4/6)")
      // Fifth => create configuration with new name
      _ <- serviceConfigurationRepository.createNewServiceConfiguration(configuration).mapError(mapDatabaseErrorToResponseError)
      _ <- ZIO.logInfo(s"Rename existing configuration $environment $serviceName to ${configuration.env} ${configuration.name}: PENDING 5/6)")
      // Sixth => start monitoring according to configuration
      _ <- monitoringController.restartForService(configuration.env, configuration.name)
      _ <- ZIO.logInfo(s"Rename existing configuration $environment $serviceName to ${configuration.env} ${configuration.name}: DONE 6/6)")
    } yield ()
  }

  override def setMaintenanceMessage(environment: String, serviceName: String, maintenanceMessage: String): IO[ErrorResponse, Unit] = for {
    oldConfiguration <- getServiceConfiguration(environment, serviceName)
    configurationWithMaintenanceMessage <- ZIO.succeed(oldConfiguration.record.copy(maintenanceMessage = maintenanceMessage))
    _ <- updateExistingServiceConfiguration(environment, serviceName, configurationWithMaintenanceMessage)
    _ <- monitoringController.restartForService(environment, serviceName)
  } yield ()

  override def setTemporaryStatus(environment: String, serviceName: String, status: RawStatus, maintenanceMessage: String): IO[ErrorResponse, Unit] = for {
    oldConfiguration <- getServiceConfiguration(environment, serviceName)
    originalCheck <- ZIO.succeed {
      oldConfiguration.record.statusCheckAction match {
        case TemporaryFixedStatus(_, originalCheck) => originalCheck // temporary status not to be nested, also handles idempotence
        case originalCheck @ _ => originalCheck
      }
    }
    temporaryStatus <- ZIO.succeed {
      TemporaryFixedStatus(
        status = status,
        originalCheck = originalCheck
      )
    }
    configurationWithTemporaryStatus <- ZIO.succeed {
      oldConfiguration.record.copy(
        statusCheckAction = temporaryStatus,
        maintenanceMessage = maintenanceMessage
      )
    }
    _ <- updateExistingServiceConfiguration(environment, serviceName, configurationWithTemporaryStatus)
    _ <- monitoringController.restartForService(environment, serviceName)
  } yield ()

  override def restoreFromTemporaryStatus(environment: String, serviceName: String): IO[ErrorResponse, Unit] = for {
    oldConfiguration <- getServiceConfiguration(environment, serviceName)
    _ <- oldConfiguration.record.statusCheckAction match {
      case TemporaryFixedStatus(_, originalCheck) => for {
        configurationWithRestoredStatusCheck <- ZIO.succeed {
          oldConfiguration.record.copy(
            maintenanceMessage = "",
            statusCheckAction = originalCheck
          )
        }
        _ <- updateExistingServiceConfiguration(environment, serviceName, configurationWithRestoredStatusCheck)
        _ <- monitoringController.restartForService(environment, serviceName)
      } yield ()
      case _ => ZIO.unit
    }
  } yield ()

  override def deleteServiceConfiguration(environment: String, serviceName: String): IO[ErrorResponse, Unit] = for {
    // First => remove configuration so its monitoring can't be started anymore
    _ <- serviceConfigurationRepository.deleteServiceConfiguration(environment, serviceName).mapError(mapDatabaseErrorToResponseError)
    // Second => stop monitoring, so its state is not updated anymore
    _ <- monitoringController.restartForService(environment, serviceName)
    // Before-Third => determine whether last state is under monitoring
    lastStateUnderMonitoring <- statusRepository.getLatestStatus(environment, serviceName)
      .flatMap(status => ZIO.succeed(status.status != Black()))
      .catchAll {
        case RecordNotFoundDatabaseError(_) => ZIO.succeed(false)
        case err => ZIO.fail(mapDatabaseErrorToResponseError(err))
      }
    // Third => mark state as Black if it was under monitoring before
    notMonitoredStatus <- ZIO.succeed {
      val instantNow = Instant.now
      RefinedStatus(
        serviceName = serviceName,
        env = environment,
        status = Black(),
        maintenanceMessage = "",
        firstSeen = instantNow,
        lastSeen = instantNow,
        notificationSent = false)
    }
    _ <- ZIO.when(lastStateUnderMonitoring)(
      statusRepository.createOrUpdate(notMonitoredStatus).mapError(mapDatabaseErrorToResponseError)
    )
    _ <- ZIO.logInfo(s"Deleted configuration $environment $serviceName")
  } yield ()

  private def hasRepositoryNotificationAction(configuration: ServiceConfiguration) = configuration.notificationAction.exists(action => action match {
    case _: NotificationAction.Repository => true
    case _: NotificationAction => false
  })
}

object ServiceConfigurationControllerImpl {
  private def mapDatabaseErrorToResponseError(error: DatabaseError): ErrorResponse = error match {
    case RecordNotFoundDatabaseError(message) => RecordNotFoundErrorResponse(message)
    case DataConflictDatabaseError(message) => DataConflictErrorResponse(message)
    case GeneralDatabaseError(message) => InternalServerErrorResponse(message)
    case unexpected => InternalServerErrorResponse(s"FATAL: ${unexpected.message}")
  }

  private def extractDependencies(item: CompositionItem): Seq[ServiceConfigurationReference] = {
    item match {
      case CompositionItem.Direct(ref) => Seq(ref)
      case CompositionItem.Partial(dependencies, _) => dependencies.flatMap(extractDependencies)
    }
  }

  val layer: RLayer[ServiceConfigurationRepository with StatusRepository with NotificationRepository with MonitoringController, ServiceConfigurationController] = ZLayer {
    for {
      serviceConfigurationRepository <- ZIO.service[ServiceConfigurationRepository]
      statusRepository <- ZIO.service[StatusRepository]
      notificationRepository <- ZIO.service[NotificationRepository]
      monitoringController <- ZIO.service[MonitoringController]
    } yield new ServiceConfigurationControllerImpl(serviceConfigurationRepository, statusRepository, notificationRepository, monitoringController)
  }
}
