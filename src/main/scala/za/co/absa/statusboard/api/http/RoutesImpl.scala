package za.co.absa.statusboard.api.http

import org.http4s.HttpRoutes
import sttp.tapir.{Endpoint, PublicEndpoint}
import sttp.tapir.server.http4s.Http4sServerOptions
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.server.interceptor.decodefailure.DecodeFailureHandler
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir._
import za.co.absa.statusboard.api.controllers.{AuthController, MonitoringController, ServiceConfigurationController, StatusController}
import za.co.absa.statusboard.api.http.Constants.{SwaggerApiName, SwaggerApiVersion}
import za.co.absa.statusboard.api.http.Endpoints._
import za.co.absa.statusboard.api.http.RoutesImpl.bindEndpoint
import za.co.absa.statusboard.config.{HttpMonitoringConfig, JvmMonitoringConfig}
import za.co.absa.statusboard.model.{ErrorResponse, RawStatus, ServiceConfiguration}
import zio._
import zio.interop.catz._
import zio.metrics.connectors.prometheus.PrometheusPublisher

class RoutesImpl(
  httpMonitoringConfig: HttpMonitoringConfig,
  jvmMonitoringConfig: JvmMonitoringConfig,
  serviceConfigurationsController: ServiceConfigurationController,
  monitoringController: MonitoringController,
  statusController: StatusController,
  authController: AuthController,
  prometheusPublisher: PrometheusPublisher,
  decodeFailureHandler: DecodeFailureHandler[Task]
) extends Routes {
  private val maintenanceEndpoints: List[ZServerEndpoint[Any, Any]] = {
    val endpointHealth = List(bindEndpoint(health, (_: Unit) => ZIO.succeed("I'm alive")))
    val endpointHttpMonitoring = List(HttpMetrics.prometheusMetrics.metricsEndpoint)
    val endpointZioMetrics = List(bindEndpoint(zioMetrics, (_: Unit) => prometheusPublisher.get))
    endpointHealth ++
      (if (httpMonitoringConfig.enabled) endpointHttpMonitoring else Nil) ++
      (if (jvmMonitoringConfig.enabled) endpointZioMetrics else Nil)
  }

  private val serviceConfigurationEndpoints: List[ZServerEndpoint[Any, Any]] = List(
    bindEndpoint(getServiceConfigurations, serviceConfigurationsController.getServiceConfigurations),
    bindEndpoint(getServiceConfigurationByName, (args: (String, String))  => serviceConfigurationsController.getServiceConfiguration(args._1, args._2)),
    bindEndpoint(getServiceConfigurationDependencies, (args: (String, String))  => serviceConfigurationsController.getServiceConfigurationDependencies(args._1, args._2)),
    bindEndpoint(getServiceConfigurationDependents, (args: (String, String))  => serviceConfigurationsController.getServiceConfigurationDependents(args._1, args._2)),
    bindEndpoint(authController)(postServiceConfiguration, serviceConfigurationsController.createNewServiceConfiguration),
    bindEndpoint(authController)(putServiceConfiguration, (args: (String, String, ServiceConfiguration)) => serviceConfigurationsController.updateExistingServiceConfiguration(args._1, args._2, args._3)),
    bindEndpoint(authController)(postServiceConfigurationWithRename, (args: (String, String, ServiceConfiguration)) => serviceConfigurationsController.renameExistingServiceConfiguration(args._1, args._2, args._3)),
    bindEndpoint(authController)(putServiceConfigurationMaintenanceMessage, (args: (String, String, String)) => serviceConfigurationsController.setMaintenanceMessage(args._1, args._2, args._3)),
    bindEndpoint(authController)(putServiceConfigurationTemporaryState, (args: (String, String, RawStatus, String)) => serviceConfigurationsController.setTemporaryStatus(args._1, args._2, args._3, args._4)),
    bindEndpoint(authController)(postServiceConfigurationRestoreFromTemporaryState, (args: (String, String)) => serviceConfigurationsController.restoreFromTemporaryStatus(args._1, args._2)),
    bindEndpoint(authController)(deleteServiceConfiguration, (args: (String, String)) => serviceConfigurationsController.deleteServiceConfiguration(args._1, args._2)),
  )

  private val statusEndpoints: List[ZServerEndpoint[Any, Any]] = List(
    bindEndpoint(getLatestServiceStatusByName, (args: (String, String))  => statusController.getLatestStatus(args._1, args._2)),
    bindEndpoint(getServiceStatusHistoryByName, (args: (String, String))  => statusController.getAllStatuses(args._1, args._2)),
    bindEndpoint(getLatestStatusOfAllActiveServices, (_: Unit) => statusController.getLatestStatusOfAllActiveConfigurations())
  )

  private val monitoringEndpoints: List[ZServerEndpoint[Any, Any]] = List(
    bindEndpoint(authController)(postRestartMonitoring, (_: Unit) => monitoringController.restart),
    bindEndpoint(authController)(postRestartMonitoringForServiceConfiguration, (args: (String, String)) => monitoringController.restartForService(args._1, args._2)),
  )

  private val swaggerEndpoints: List[ZServerEndpoint[Any, Any]] = {
    val endpoints = List(
      health,
      zioMetrics,
      getServiceConfigurations,
      getServiceConfigurationByName,
      postServiceConfiguration,
      putServiceConfiguration,
      deleteServiceConfiguration,
      getLatestServiceStatusByName,
      getServiceStatusHistoryByName,
      postRestartMonitoring,
      postRestartMonitoringForServiceConfiguration
    )
    SwaggerInterpreter().fromEndpoints[Task](endpoints, SwaggerApiName, SwaggerApiVersion)
  }

  override val routes: HttpRoutes[Task] = {
    val optionsBuilder = Http4sServerOptions.customiseInterceptors[Task].decodeFailureHandler(decodeFailureHandler)
    val options = if (httpMonitoringConfig.enabled)
      optionsBuilder.metricsInterceptor(HttpMetrics.prometheusMetrics.metricsInterceptor()).options
    else
      optionsBuilder.options
    val allEndpoints = maintenanceEndpoints ++ serviceConfigurationEndpoints ++ statusEndpoints ++ monitoringEndpoints ++ swaggerEndpoints
    val rawRoutes = ZHttp4sServerInterpreter(options).from(allEndpoints).toRoutes
    rawRoutes.asInstanceOf[HttpRoutes[Task]] // IntelliJ can't infer type properly
  }
}

object RoutesImpl {
  private def bindEndpoint[I, E, O](endpoint: PublicEndpoint[I, E, O, Any], logic: I => ZIO[Any, E, O]): ZServerEndpoint[Any, Any] =
    endpoint.zServerLogic(logic)

  private def bindEndpoint[I, O](authController: AuthController)(endpoint: Endpoint[String, I, ErrorResponse, O, Any], logic: I => ZIO[Any, ErrorResponse, O]): ZServerEndpoint[Any, Any] =
    endpoint.zServerSecurityLogic[Any, Any](authController.authorize).serverLogic[Any](_ => logic)

  val layer: RLayer[ServiceConfigurationController with MonitoringController with StatusController with AuthController with PrometheusPublisher, Routes] = ZLayer {
    for {
      httpMonitoringConfig <- ZIO.config[HttpMonitoringConfig](HttpMonitoringConfig.config)
      jvmMonitoringConfig <- ZIO.config[JvmMonitoringConfig](JvmMonitoringConfig.config)
      serviceConfigurationsController <- ZIO.service[ServiceConfigurationController]
      monitoringController <- ZIO.service[MonitoringController]
      statusController <- ZIO.service[StatusController]
      authController <- ZIO.service[AuthController]
      prometheusPublisher <- ZIO.service[PrometheusPublisher]
      decodeFailureHandler <- ZIO.succeed(DecodeFailureHandlerImpl)
    } yield new RoutesImpl(
      httpMonitoringConfig,
      jvmMonitoringConfig,
      serviceConfigurationsController,
      monitoringController,
      statusController,
      authController,
      prometheusPublisher,
      decodeFailureHandler
    )
  }
}
