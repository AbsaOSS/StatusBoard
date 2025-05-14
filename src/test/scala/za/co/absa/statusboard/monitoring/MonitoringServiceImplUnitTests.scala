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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import za.co.absa.statusboard.model.AppError.DatabaseError.RecordNotFoundDatabaseError
import za.co.absa.statusboard.model.ServiceConfiguration
import za.co.absa.statusboard.repository.ServiceConfigurationRepository
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio.concurrent.ConcurrentSet
import zio.test.Assertion.{hasSameElements, isUnit}
import zio.test.{Spec, TestAspect, TestEnvironment, assertZIO}
import zio.{Scope, Task, ZIO, ZLayer}

import java.time.Duration

object MonitoringServiceImplUnitTests extends ConfigProviderSpec {
  private val serviceBase = TestData.serviceConfiguration.copy(statusCheckIntervalSeconds = 1, description = "0")
  private val serviceAlpha = serviceBase.copy(name = "A", statusCheckIntervalSeconds = 1)
  private val serviceBravo = serviceBase.copy(name = "B", statusCheckIntervalSeconds = 1)
  private val serviceCharlie = serviceBase.copy(name = "C", statusCheckIntervalSeconds = 1)
  private val repositoryMock = mock(classOf[ServiceConfigurationRepository])
  private val monitoringWorkerMock = mock(classOf[MonitoringWorker])

  private def setupMocks(liveItems: ConcurrentSet[String], services: Seq[ServiceConfiguration]): Task[Unit] = for {
    _ <- ZIO.attempt {
      reset(repositoryMock)
      reset(monitoringWorkerMock)
      when(repositoryMock.getServiceConfigurations)
        .thenReturn(ZIO.succeed(services))
      when(repositoryMock.getServiceConfiguration(any[String], any[String]))
        .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("BAKA")))
      when(monitoringWorkerMock.performMonitoringWork(ArgumentMatchers.any[ServiceConfiguration]))
        .thenAnswer(x => liveItems.add(
          s"${x.getArgument[ServiceConfiguration](0).name}${x.getArgument[ServiceConfiguration](0).description}"
        ).unit)
    }
    _ <- ZIO.foreachDiscard(services) { service =>
      ZIO.attempt {
        when(repositoryMock.getServiceConfiguration("TestEnv", service.name))
          .thenReturn(ZIO.succeed(service))
      }
    }
  } yield ()


  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("MonitoringServiceImplSuite")(
      suite("restart")(
        test("cold restart on empty repository should survive - i.e. run nothing") {
          assertZIO(for {
            // Arrange
            liveItems <- ConcurrentSet.empty[String]
            _ <- setupMocks(liveItems, Seq.empty[ServiceConfiguration])
            // Act
            _ <- MonitoringService.restart
            // Assert
            _ <- assertZIO(liveItems.toSet.delay(Duration.ofSeconds(2)))(hasSameElements(Seq.empty[String]))
          } yield ())(isUnit)
        },
        test("cold restart should start worker threads") {
          assertZIO(for {
            // Arrange
            liveItems <- ConcurrentSet.empty[String]
            _ <- setupMocks(liveItems, Seq(serviceAlpha, serviceBravo))
            // Act
            _ <- MonitoringService.restart
            // Assert
            _ <- assertZIO(liveItems.toSet.delay(Duration.ofSeconds(2)))(hasSameElements(Seq("A0", "B0")))
            // Clean
            _ <- MonitoringService.stop
          } yield ())(isUnit)
        },
        test("warm restart should kill all worker threads and start new ones (i.e. remove removed, add added, cycle existing)") {
          assertZIO(for {
            // Arrange
            liveItems <- ConcurrentSet.empty[String]
            _ <- setupMocks(liveItems, Seq(serviceAlpha, serviceBravo))
            _ <- MonitoringService.restart
            _ <- assertZIO(liveItems.toSet.delay(Duration.ofSeconds(2)))(hasSameElements(Seq("A0", "B0")))
            _ <- setupMocks(liveItems, Seq(serviceBravo.copy(description = "1"), serviceCharlie))
            // Act
            _ <- MonitoringService.restart
            // Assert
            _ <- assertZIO(liveItems.toSet.delay(Duration.ofSeconds(2)))(hasSameElements(Seq("B1", "C0")))
            // Clean
            _ <- MonitoringService.stop
          } yield ())(isUnit)
        },
        test("warm restart on empty repository should kill worker threads") {
          assertZIO(for {
            // Arrange
            liveItems <- ConcurrentSet.empty[String]
            _ <- setupMocks(liveItems, Seq(serviceAlpha, serviceBravo))
            _ <- MonitoringService.restart
            _ <- assertZIO(liveItems.toSet.delay(Duration.ofSeconds(2)))(hasSameElements(Seq("A0", "B0")))
            _ <- setupMocks(liveItems, Seq.empty[ServiceConfiguration])
            // Act
            _ <- MonitoringService.restart
            // Assert
            _ <- assertZIO(liveItems.toSet.delay(Duration.ofSeconds(2)))(hasSameElements(Seq.empty[String]))
          } yield ())(isUnit)
        },
      ),
      suite("stop")(
        test("stop should survive when nothing runs") {
          assertZIO(for {
            liveItems <- ConcurrentSet.empty[String]
            // Arrange
            _ <- setupMocks(liveItems, Seq.empty[ServiceConfiguration])
            // Act
            _ <- MonitoringService.stop
            // Assert
            _ <- assertZIO(liveItems.toSet.delay(Duration.ofSeconds(2)))(hasSameElements(Seq.empty[String]))
          } yield ())(isUnit)
        },
        test("stop should kill all worker threads") {
          assertZIO(for {
            // Arrange
            liveItems <- ConcurrentSet.empty[String]
            _ <- setupMocks(liveItems, Seq(serviceAlpha, serviceBravo))
            _ <- MonitoringService.restart
            _ <- assertZIO(liveItems.toSet.delay(Duration.ofSeconds(2)))(hasSameElements(Seq("A0", "B0")))
            // Act
            _ <- MonitoringService.stop
            // Assert
            _ <- assertZIO(liveItems.toSet.delay(Duration.ofSeconds(2)))(hasSameElements(Seq.empty[String]))
          } yield ())(isUnit)
        }
      ),
      suite("restartForService")(
        test("restartForService should kill worker thread for service no longer in repo") {
          assertZIO(for {
            // Arrange
            liveItems <- ConcurrentSet.empty[String]
            _ <- setupMocks(liveItems, Seq(serviceAlpha, serviceBravo))
            _ <- MonitoringService.restart
            _ <- assertZIO(liveItems.toSet.delay(Duration.ofSeconds(2)))(hasSameElements(Seq("A0", "B0")))
            _ <- setupMocks(liveItems, Seq(serviceBravo.copy(description = "1"), serviceCharlie))
            // Act
            _ <- MonitoringService.restartForService("TestEnv", "A")
            // Assert
            _ <- assertZIO(liveItems.toSet.delay(Duration.ofSeconds(2)))(hasSameElements(Seq("B0")))
            // Clean
            _ <- MonitoringService.stop
          } yield ())(isUnit)
        },
        test("restartForService should cycle worker thread for given service") {
          assertZIO(for {
            // Arrange
            liveItems <- ConcurrentSet.empty[String]
            _ <- setupMocks(liveItems, Seq(serviceAlpha, serviceBravo))
            _ <- MonitoringService.restart
            _ <- assertZIO(liveItems.toSet.delay(Duration.ofSeconds(2)))(hasSameElements(Seq("A0", "B0")))
            _ <- setupMocks(liveItems, Seq(serviceBravo.copy(description = "1"), serviceCharlie))
            // Act
            _ <- MonitoringService.restartForService("TestEnv", "B")
            // Assert
            _ <- assertZIO(liveItems.toSet.delay(Duration.ofSeconds(2)))(hasSameElements(Seq("A0", "B1")))
            // Clean
            _ <- MonitoringService.stop
          } yield ())(isUnit)
        },
        test("restartForService should start worker thread for new service") {
          assertZIO(for {
            // Arrange
            liveItems <- ConcurrentSet.empty[String]
            _ <- setupMocks(liveItems, Seq(serviceAlpha, serviceBravo))
            _ <- MonitoringService.restart
            _ <- assertZIO(liveItems.toSet.delay(Duration.ofSeconds(2)))(hasSameElements(Seq("A0", "B0")))
            _ <- setupMocks(liveItems, Seq(serviceBravo.copy(description = "1"), serviceCharlie))
            // Act
            _ <- MonitoringService.restartForService("TestEnv", "C")
            // Assert
            _ <- assertZIO(liveItems.toSet.delay(Duration.ofSeconds(2)))(hasSameElements(Seq("A0", "B0", "C0")))
            // Clean
            _ <- MonitoringService.stop
          } yield ())(isUnit)
        }
      )
    ) @@ TestAspect.sequential @@ TestAspect.withLiveClock
  }.provide(MonitoringServiceImpl.layer, ZLayer.succeed(repositoryMock), ZLayer.succeed(monitoringWorkerMock))
}
