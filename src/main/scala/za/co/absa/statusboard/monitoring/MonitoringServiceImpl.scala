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

package za.co.absa.statusboard.monitoring

import za.co.absa.statusboard.model.AppError.DatabaseError.RecordNotFoundDatabaseError
import za.co.absa.statusboard.model.ServiceConfiguration
import za.co.absa.statusboard.monitoring.MonitoringServiceImpl.fullServiceName
import za.co.absa.statusboard.repository.ServiceConfigurationRepository
import zio._
import zio.concurrent.ConcurrentMap

import java.time.Duration

class MonitoringServiceImpl(
  serviceConfigurationRepository: ServiceConfigurationRepository,
  monitoringWorker: MonitoringWorker,
  liveMonitoringFibers: ConcurrentMap[String, Fiber.Runtime[Throwable, Long]]
) extends MonitoringService {
  override def restart: Task[Unit] = {
    for {
      _ <- ZIO.logInfo("(Re)Starting monitoring service")
      _ <- stop
      serviceConfigurations <- serviceConfigurationRepository.getServiceConfigurations
      _ <- ZIO.foreachParDiscard(serviceConfigurations)(startMonitoringFibre)
      _ <- ZIO.logInfo("Started monitoring service")
    } yield ()
  }

  override def stop: Task[Unit] = {
    for {
      _ <- ZIO.logInfo("Interrupting all existing monitoring tasks")
      liveFibersList <- liveMonitoringFibers.toList
      _ <- ZIO.foreachParDiscard(liveFibersList)(liveFiber => removeFibre(liveFiber._1))
      _ <- ZIO.logInfo("Interrupted all existing monitoring tasks")
      isEmptyNow <- liveMonitoringFibers.isEmpty
      _ <- stop.unless(isEmptyNow)
    } yield ()
  }

  override def restartForService(environment: String, serviceName: String): Task[Unit] = {
    for {
      _ <- ZIO.logInfo(s"Restarting monitoring for ${fullServiceName(environment, serviceName)}")
      serviceConfigurationOrFail <- serviceConfigurationRepository.getServiceConfiguration(environment, serviceName).either
      _ <- serviceConfigurationOrFail match {
        case Right(serviceConfiguration) => startMonitoringFibre(serviceConfiguration)
        case Left(RecordNotFoundDatabaseError(_)) => removeFibre(fullServiceName(environment, serviceName))
        case Left(other_error) => ZIO.fail(other_error)
      }
    } yield ()
  }

  private def removeFibre(fullServiceName: String): Task[Unit] = {
    for {
      maybeFiber <- liveMonitoringFibers.remove(fullServiceName)
      _ <- maybeFiber match {
        case None => ZIO.unit
        case Some(fiber) => fiber.interrupt *> ZIO.logInfo(s"Interrupted monitoring fiber for $fullServiceName: $fiber")
      }
    } yield ()
  }

  private def startMonitoringFibre(serviceConfiguration: ServiceConfiguration): Task[Unit] = {
    for {
      fullServiceName <- ZIO.succeed(fullServiceName(serviceConfiguration))
      _ <- removeFibre(fullServiceName) // potential non-racing previous version
      liveFiber <- monitoringWorker.performMonitoringWork(serviceConfiguration).repeat(Schedule.forever).forkDaemon // Delay is variable based on result, so sleep is inside performMonitoringWork
      _ <- ZIO.logInfo(s"Started monitoring fiber for $fullServiceName: $liveFiber")
      maybeRacerFiber <- liveMonitoringFibers.put(fullServiceName, liveFiber)
      _ <- maybeRacerFiber match {
        case None => ZIO.unit
        case Some(racerFiber) => racerFiber.interrupt *> ZIO.logInfo(s"Interrupted racer monitoring fiber for $fullServiceName: $racerFiber")
      }
    } yield ()
  }
}

object MonitoringServiceImpl {
  private def fullServiceName(environment: String, serviceName: String): String = s"$environment / $serviceName"
  private def fullServiceName(configuration: ServiceConfiguration): String = fullServiceName(configuration.env, configuration.name)

  val layer: ZLayer[ServiceConfigurationRepository with MonitoringWorker, Throwable, MonitoringService] = ZLayer {
    for {
      serviceConfigurationRepository <- ZIO.service[ServiceConfigurationRepository]
      monitoringWorker <- ZIO.service[MonitoringWorker]
      liveMonitoringFibers <- ConcurrentMap.empty[String, Fiber.Runtime[Throwable, Long]]
    } yield new MonitoringServiceImpl(serviceConfigurationRepository, monitoringWorker, liveMonitoringFibers)
  }
}
