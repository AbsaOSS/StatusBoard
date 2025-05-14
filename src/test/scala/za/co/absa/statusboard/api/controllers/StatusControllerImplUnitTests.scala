package za.co.absa.statusboard.api.controllers

import org.mockito.Mockito.{mock, _}
import za.co.absa.statusboard.model.ApiResponse.{MultiApiResponse, SingleApiResponse}
import za.co.absa.statusboard.model.AppError.DatabaseError.{GeneralDatabaseError, RecordNotFoundDatabaseError}
import za.co.absa.statusboard.model.ErrorResponse.{InternalServerErrorResponse, RecordNotFoundErrorResponse}
import za.co.absa.statusboard.model.RefinedStatus
import za.co.absa.statusboard.repository.StatusRepository
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio._
import zio.test.Assertion.{equalTo, failsWithA}
import zio.test._

object StatusControllerImplUnitTests extends ConfigProviderSpec {
  private val statusRepositoryMock = mock(classOf[StatusRepository])

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("StatusControllerImplSuite")(
      test("getAllStatuses passes call to repository") {
        for {
          // Arrange
          _ <- ZIO.attempt {
            reset(statusRepositoryMock)
            when(statusRepositoryMock.getAllStatuses("TestEnv", "TestService"))
              .thenReturn(ZIO.succeed(Seq.empty[RefinedStatus]))
          }
          // Act
          result <- StatusController.getAllStatuses("TestEnv", "TestService")
          // Assert
          _ <- ZIO.attempt(verify(statusRepositoryMock, times(1)).getAllStatuses("TestEnv", "TestService"))
        } yield assert(result)(equalTo(MultiApiResponse(Seq.empty[RefinedStatus])))
      },
      test("getAllStatuses failure presents InternalServerError") {
        for {
          // Arrange
          _ <- ZIO.attempt {
            reset(statusRepositoryMock)
            when(statusRepositoryMock.getAllStatuses("TestEnv", "TestService"))
              .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
          }
          // Act
          failure <- StatusController.getAllStatuses("TestEnv", "TestService").exit
          // Assert
          _ <- ZIO.attempt(verify(statusRepositoryMock, times(1)).getAllStatuses("TestEnv", "TestService"))
        } yield assert(failure)(failsWithA[InternalServerErrorResponse])
      },
      test("getLatestStatus passes call to repository") {
        for {
          // Arrange
          _ <- ZIO.attempt {
            reset(statusRepositoryMock)
            when(statusRepositoryMock.getLatestStatus("TestEnv", "TestService"))
              .thenReturn(ZIO.succeed(TestData.refinedStatus))
          }
          // Act
          result <- StatusController.getLatestStatus("TestEnv", "TestService")
          // Assert
          _ <- ZIO.attempt(verify(statusRepositoryMock, times(1)).getLatestStatus("TestEnv", "TestService"))
        } yield assert(result)(equalTo(SingleApiResponse(TestData.refinedStatus)))
      },
      test("getLatestStatus not found presents NotFoundResponse") {
        for {
          // Arrange
          _ <- ZIO.attempt {
            reset(statusRepositoryMock)
            when(statusRepositoryMock.getLatestStatus("TestEnv", "TestService"))
              .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("NotPresent")))
          }
          // Act
          failure <- StatusController.getLatestStatus("TestEnv", "TestService").exit
          // Assert
          _ <- ZIO.attempt(verify(statusRepositoryMock, times(1)).getLatestStatus("TestEnv", "TestService"))
        } yield assert(failure)(failsWithA[RecordNotFoundErrorResponse])
      },
      test("getLatestStatus failure presents InternalServerError") {
        for {
          // Arrange
          _ <- ZIO.attempt {
            reset(statusRepositoryMock)
            when(statusRepositoryMock.getLatestStatus("TestEnv", "TestService"))
              .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
          }
          // Act
          failure <- StatusController.getLatestStatus("TestEnv", "TestService").exit
          // Assert
          _ <- ZIO.attempt(verify(statusRepositoryMock, times(1)).getLatestStatus("TestEnv", "TestService"))
        } yield assert(failure)(failsWithA[InternalServerErrorResponse])
      },
      test("getLatestStatusOfAllActiveConfigurations passes call to repository") {
        for {
          // Arrange
          _ <- ZIO.attempt {
            reset(statusRepositoryMock)
            when(statusRepositoryMock.getLatestStatusOfAllActiveConfigurations())
              .thenReturn(ZIO.succeed(Set.empty[RefinedStatus]))
          }
          // Act
          result <- StatusController.getLatestStatusOfAllActiveConfigurations()
          // Assert
          _ <- ZIO.attempt(verify(statusRepositoryMock, times(1)).getLatestStatusOfAllActiveConfigurations())
        } yield assert(result)(equalTo(MultiApiResponse(Seq.empty[RefinedStatus])))
      },
      test("getLatestStatusOfAllActiveConfigurations failure presents InternalServerError") {
        for {
          // Arrange
          _ <- ZIO.attempt {
            reset(statusRepositoryMock)
            when(statusRepositoryMock.getLatestStatusOfAllActiveConfigurations())
              .thenReturn(ZIO.fail(GeneralDatabaseError("BAKA")))
          }
          // Act
          failure <- StatusController.getLatestStatusOfAllActiveConfigurations().exit
          // Assert
          _ <- ZIO.attempt(verify(statusRepositoryMock, times(1)).getLatestStatusOfAllActiveConfigurations())
        } yield assert(failure)(failsWithA[InternalServerErrorResponse])
      },
    ) @@ TestAspect.sequential
  }.provide(StatusControllerImpl.layer, ZLayer.succeed(statusRepositoryMock))
}
