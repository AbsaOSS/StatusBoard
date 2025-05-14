package za.co.absa.statusboard.repository

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import za.co.absa.statusboard.config.RepositoriesConfig
import za.co.absa.statusboard.model.AppError.DatabaseError.RecordNotFoundDatabaseError
import za.co.absa.statusboard.model.{RawStatus, RefinedStatus}
import za.co.absa.statusboard.providers.DynamoDbProvider
import za.co.absa.statusboard.testUtils.TestData.rawStatusRed
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import za.co.absa.statusboard.utils.DynamoDbUtils.DynamoDbExtensions
import zio._
import zio.test.Assertion.{equalTo, failsWithA, isUnit}
import zio.test._

import java.time.Instant

object DynamoDbStatusRepositoryDynamoDBIntegrationTests extends ConfigProviderSpec {
  private val refinedStatus: RefinedStatus = TestData.refinedStatus

  private val refinedStatusUpdated: RefinedStatus = refinedStatus.copy(
    lastSeen = Instant.parse("2024-05-17T14:00:00Z"),
    notificationSent = true);

  private val refinedStatusAnother: RefinedStatus = refinedStatus.copy(
    status = rawStatusRed,
    firstSeen = Instant.parse("2024-05-17T15:00:00Z"),
    lastSeen = Instant.parse("2024-05-17T16:00:00Z"))

  private val refinedStatusAnotherRenamed: RefinedStatus = refinedStatusAnother.copy(
    serviceName = "Renamed Test Service")

  private val refinedStatusAnotherService: RefinedStatus = refinedStatus.copy(
    serviceName = "Different Test Service")

  private val refinedStatusDeadService: RefinedStatus = refinedStatus.copy(
    status = RawStatus.Black(),
    serviceName = "Dead Test Service")


  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("DynamoDbStatusRepositorySuite")(
      test("getLatestStatus should retrieve nothing when no state exist") {
        assertZIO(StatusRepository.getLatestStatus("TestEnv", "NonExistentService").exit)(
          failsWithA[RecordNotFoundDatabaseError]
        )
      },
      test("getAllStatuses should retrieve nothing when no state exist") {
        assertZIO(StatusRepository.getAllStatuses("TestEnv", "NonExistentService"))(
          equalTo(List.empty[RefinedStatus])
        )
      },
      test("getLatestStatusOfAllActiveConfigurations should retrieve nothing when no state exist") {
        assertZIO(StatusRepository.getLatestStatusOfAllActiveConfigurations())(
          equalTo(Set.empty[RefinedStatus])
        )
      },
      test("createOrUpdate should not fail when creating first state") {
        assertZIO(StatusRepository.createOrUpdate(refinedStatus))(isUnit)
      },
      test("createOrUpdate should not fail when creating first state for another service") {
        assertZIO(StatusRepository.createOrUpdate(refinedStatusAnotherService))(isUnit)
      },
      test("getLatestStatus should retrieve nothing when no state exist for anotherService") {
        assertZIO(StatusRepository.getLatestStatus("TestEnv", "NonExistentService").exit)(
          failsWithA[RecordNotFoundDatabaseError]
        )
      },
      test("getAllStatuses should retrieve nothing when no state exist for anotherService") {
        assertZIO(StatusRepository.getAllStatuses("TestEnv", "NonExistentService"))(
          equalTo(List.empty[RefinedStatus])
        )
      },
      test("getLatestStatus should retrieve last status") {
        assertZIO(StatusRepository.getLatestStatus(refinedStatus.env, refinedStatus.serviceName))(
          equalTo(refinedStatus)
        )
      },
      test("getAllStatuses should retrieve last status") {
        assertZIO(StatusRepository.getAllStatuses(refinedStatus.env, refinedStatus.serviceName))(
          equalTo(List(refinedStatus))
        )
      },
      test("getLatestStatusOfAllActiveConfigurations should retrieve last status for existing service") {
        assertZIO(StatusRepository.getLatestStatusOfAllActiveConfigurations())(
          equalTo(Set(refinedStatusAnotherService, refinedStatus))
        )
      },
      test("createOrUpdate should not fail when creating *updated* state for service") {
        assertZIO(StatusRepository.createOrUpdate(refinedStatusUpdated))(isUnit)
      },
      test("getLatestStatus should retrieve single last status") {
        assertZIO(StatusRepository.getLatestStatus(refinedStatusUpdated.env, refinedStatusUpdated.serviceName))(
          equalTo(refinedStatusUpdated)
        )
      },
      test("getAllStatuses should retrieve single last status") {
        assertZIO(StatusRepository.getAllStatuses(refinedStatusUpdated.env, refinedStatusUpdated.serviceName))(
          equalTo(List(refinedStatusUpdated))
        )
      },
      test("getLatestStatusOfAllActiveConfigurations should retrieve single last status for existing service") {
        assertZIO(StatusRepository.getLatestStatusOfAllActiveConfigurations())(
          equalTo(Set(refinedStatusAnotherService, refinedStatusUpdated))
        )
      },
      test("createOrUpdate should not fail when creating *new* state for service") {
        assertZIO(StatusRepository.createOrUpdate(refinedStatusAnother))(isUnit)
      },
      test("getLatestStatus should retrieve last status") {
        assertZIO(StatusRepository.getLatestStatus(refinedStatusAnother.env, refinedStatusAnother.serviceName))(
          equalTo(refinedStatusAnother)
        )
      },
      test("getAllStatuses should retrieve both last statuses") {
        assertZIO(StatusRepository.getAllStatuses(refinedStatusUpdated.env, refinedStatusUpdated.serviceName))(
          equalTo(List(refinedStatusUpdated, refinedStatusAnother))
        )
      },
      test("getLatestStatusOfAllActiveConfigurations should retrieve last statuses for existing services") {
        assertZIO(StatusRepository.getLatestStatusOfAllActiveConfigurations())(
          equalTo(Set(refinedStatusAnotherService, refinedStatusAnother))
        )
      },
      test("createOrUpdate should not fail when creating dead service") {
        assertZIO(StatusRepository.createOrUpdate(refinedStatusDeadService))(isUnit)
      },
      test("getLatestStatusOfAllActiveConfigurations should retrieve last statuses for existing active services") {
        assertZIO(StatusRepository.getLatestStatusOfAllActiveConfigurations())(
          equalTo(Set(refinedStatusAnotherService, refinedStatusAnother))
        )
      },
      test("renameService should not fail when renaming service") {
        assertZIO(StatusRepository.renameService(refinedStatusAnother.env, refinedStatusAnother.serviceName, refinedStatusAnotherRenamed.env, refinedStatusAnotherRenamed.serviceName))(isUnit)
      },
      test("getLatestStatusOfAllActiveConfigurations should retrieve last statuses for existing active services") {
        assertZIO(StatusRepository.getLatestStatusOfAllActiveConfigurations())(
          equalTo(Set(refinedStatusAnotherService, refinedStatusAnotherRenamed))
        )
      }
    ) @@
      TestAspect.sequential @@
      TestAspect.afterAll(deleteTable().ignore) @@
      TestAspect.withLiveClock
  }.provide(DynamoDbStatusRepository.layer, DynamoDbProvider.layer)

  private def deleteTable(): ZIO[DynamoDbClient, Throwable, Unit] = for {
    tableName <- ZIO.config[RepositoriesConfig](RepositoriesConfig.config).map(_.statusesTableName)
    _ <- ZIO.serviceWithZIO[DynamoDbClient](_.deleteTableSafe(tableName))
  } yield ()
}
