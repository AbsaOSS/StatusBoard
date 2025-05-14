package za.co.absa.statusboard.api.controllers

import org.mockito.Mockito.{mock, reset, times, verify, when}
import za.co.absa.statusboard.model.ErrorResponse.InternalServerErrorResponse
import za.co.absa.statusboard.monitoring.MonitoringService
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import zio.test.Assertion.failsWithA
import zio.test._
import zio._

object MonitoringControllerImplUnitTests extends ConfigProviderSpec {
  private val monitoringServiceMock = mock(classOf[MonitoringService])

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("MonitoringControllerImplSuite")(
      test("restart passes call to monitoring service") {
        for {
          // Arrange
          _ <- ZIO.attempt {
            reset(monitoringServiceMock)
            when(monitoringServiceMock.restart)
              .thenReturn(ZIO.unit)
          }
          // Act
          _ <- MonitoringController.restart
          // Assert
          _ <- ZIO.attempt(verify(monitoringServiceMock, times(1)).restart)
        } yield assertCompletes
      },
      test("restart failure presents InternalServerError") {
        for {
          // Arrange
          _ <- ZIO.attempt {
            reset(monitoringServiceMock)
            when(monitoringServiceMock.restart)
              .thenReturn(ZIO.fail(new Exception("BAKA")))
          }
          // Act
          failure <- MonitoringController.restart.exit
          // Assert
          _ <- ZIO.attempt(verify(monitoringServiceMock, times(1)).restart)
        } yield assert(failure)(failsWithA[InternalServerErrorResponse])
      },
      test("restartForService passes call to monitoring service") {
        for {
          // Arrange
          _ <- ZIO.attempt {
            reset(monitoringServiceMock)
            when(monitoringServiceMock.restartForService("TestEnv", "MyCoolService"))
              .thenReturn(ZIO.unit)
          }
          // Act
          _ <- MonitoringController.restartForService("TestEnv", "MyCoolService")
          // Assert
          _ <- ZIO.attempt(verify(monitoringServiceMock, times(1)).restartForService("TestEnv", "MyCoolService"))
        } yield assertCompletes
      },
      test("restartForService failure presents InternalServerError") {
        for {
          // Arrange
          _ <- ZIO.attempt {
            reset(monitoringServiceMock)
            when(monitoringServiceMock.restartForService("TestEnv", "MyNotSoCoolService"))
              .thenReturn(ZIO.fail(new Exception("BAKA")))
          }
          // Act
          failure <- MonitoringController.restartForService("TestEnv", "MyNotSoCoolService").exit
          // Assert
          _ <- ZIO.attempt(verify(monitoringServiceMock, times(1)).restartForService("TestEnv", "MyNotSoCoolService"))
        } yield assert(failure)(failsWithA[InternalServerErrorResponse])
      }
    ) @@ TestAspect.sequential
  }.provide(MonitoringControllerImpl.layer, ZLayer.succeed(monitoringServiceMock))
}
