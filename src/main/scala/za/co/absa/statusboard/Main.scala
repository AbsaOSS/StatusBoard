/*
 * Copyright 2024 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.statusboard

import za.co.absa.statusboard.api.controllers.{AuthControllerImpl, MonitoringControllerImpl, ServiceConfigurationControllerImpl, StatusControllerImpl}
import za.co.absa.statusboard.api.http.{RoutesImpl, Server}
import za.co.absa.statusboard.checker._
import za.co.absa.statusboard.config.JvmMonitoringConfig
import za.co.absa.statusboard.monitoring.{MonitoringService, MonitoringServiceImpl, MonitoringWorkerImpl}
import za.co.absa.statusboard.notification.NotificationServiceImpl
import za.co.absa.statusboard.notification.actioners.{EmailNotificationActionerImpl, MsTeamsNotificationActionerImpl, RepositoryNotificationActionerImpl}
import za.co.absa.statusboard.notification.actioners.PolymorphicNotificationActioner
import za.co.absa.statusboard.notification.deciders.{DurationBasedNotificationDeciderImpl, PolymorphicNotificationDecider}
import za.co.absa.statusboard.providers._
import za.co.absa.statusboard.repository._
import zio._
import zio.config.typesafe.TypesafeConfigProvider
import zio.logging.backend.SLF4J
import zio.metrics.connectors.{MetricsConfig, prometheus}
import zio.metrics.jvm.DefaultJvmMetrics

import java.time.Duration

object Main extends ZIOAppDefault {

  private val configProvider: ConfigProvider = TypesafeConfigProvider.fromResourcePath()

  private val myApp = {
    for {
      _ <- ZIO.logInfo("Status board launching...")
      _ <- MonitoringService.restart
      _ <- Server.start
      _ <- ZIO.logInfo("Status board launched")
      _ <- ZIO.never // TODO: obviously hook to some kill signal apart from interrupt
      _ <- ZIO.logInfo("Status board shutting down...")
      //_ <- Server.stop // TODO: immortal for now
      _ <- MonitoringService.stop
      _ <- ZIO.logInfo("Status board shut down - bye :)")
    } yield ()
  }

  override def run = {
    ZIO.config[JvmMonitoringConfig](JvmMonitoringConfig.config).flatMap { jvmMonitoringConfig =>
      myApp
        .provide(
          // REST
          RoutesImpl.layer,
          ServiceConfigurationControllerImpl.layer,
          MonitoringControllerImpl.layer,
          StatusControllerImpl.layer,
          AuthControllerImpl.layer,

          // Monitoring
          MonitoringServiceImpl.layer,
          MonitoringWorkerImpl.layer,

          // Monitoring::Checker
          PolymorphicChecker.layer,
          HttpGetRequestStatusCodeOnlyCheckerImpl.layer,
          HttpGetRequestWithMessageCheckerImpl.layer,
          HttpGetRequestWithJsonStatusCheckerImpl.layer,
          FixedStatusCheckerImpl.layer,
          TemporaryFixedStatusCheckerImpl.layer,
          CompositionCheckerImpl.layer,
          AwsRdsCheckerImpl.layer,
          AwsRdsClusterCheckerImpl.layer,
          AwsEmrCheckerImpl.layer,
          AwsEc2AmiComplianceCheckerImpl.layer,

          // Monitoring::Notification Service
          NotificationServiceImpl.layer,
          PolymorphicNotificationDecider.layer,
          DurationBasedNotificationDeciderImpl.layer,
          PolymorphicNotificationActioner.layer,
          EmailNotificationActionerImpl.layer,
          RepositoryNotificationActionerImpl.layer,
          MsTeamsNotificationActionerImpl.layer,

          // Repositories
          DynamoDbServiceConfigurationRepository.layer,
          DynamoDbStatusRepository.layer,
          DynamoDbNotificationRepository.layer,

          // Providers
          MsTeamsProviderImpl.layer,
          EmailProviderImpl.layer,
          DynamoDbProvider.layer,
          RdsClientBuilderProvider.layer,
          EmrClientBuilderProvider.layer,
          Ec2ClientBuilderProvider.layer,
          BlazeHttpClientProvider.layer,

          prometheus.publisherLayer,
          prometheus.prometheusLayer,
          // enabling conditionally collection of ZIO runtime metrics and default JVM metrics
          if (jvmMonitoringConfig.enabled) {
            ZLayer.succeed(MetricsConfig(Duration.ofSeconds(jvmMonitoringConfig.intervalInSeconds))) ++
              Runtime.enableRuntimeMetrics.unit ++ DefaultJvmMetrics.live.unit
          } else {
            ZLayer.succeed(MetricsConfig(Duration.ofSeconds(Long.MaxValue)))
          },
          zio.Scope.default
        )
    }
  }

  override val bootstrap: ZLayer[Any, Config.Error, Unit] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j >>> Runtime.setConfigProvider(configProvider)
}
