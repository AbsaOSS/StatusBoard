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

package za.co.absa.statusboard.repository

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import za.co.absa.statusboard.config.RepositoriesConfig
import za.co.absa.statusboard.model.AppError.DatabaseError.{DataConflictDatabaseError, RecordNotFoundDatabaseError}
import za.co.absa.statusboard.model.ServiceConfiguration.ServiceConfigurationLinks
import za.co.absa.statusboard.model.{NotificationAction, ServiceConfiguration}
import za.co.absa.statusboard.providers.DynamoDbProvider
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import za.co.absa.statusboard.utils.DynamoDbUtils._
import zio._
import zio.test.Assertion.{equalTo, failsWithA, hasSameElements, isUnit}
import zio.test._

import scala.jdk.CollectionConverters._

object DynamoDbServiceConfigurationRepositoryDynamoDBIntegrationTests extends ConfigProviderSpec {
  private val configuration = TestData.serviceConfiguration
  private val configurationUpdated = configuration.copy(notificationAction = Seq.empty[NotificationAction])
  private val configurationDifferentService = configuration.copy(name = "Different service")
  private val legacyConfiguration83 = configuration.copy(
    name = "pre_#83_legacy_service",
    hidden = false, // since #139
    links = ServiceConfigurationLinks("", "", "", "", ""), // since #113
    statusCheckNonGreenIntervalSeconds = configuration.statusCheckIntervalSeconds // since #83
  )
  private val legacyConfiguration113 = configuration.copy(
    name = "pre_#113_legacy_service",
    hidden = false, // since #139
    links = ServiceConfigurationLinks("", "", "", "", "") // since #113
  )
  private val legacyConfiguration139 = configuration.copy(
    name = "pre_#139_legacy_service",
    hidden = false, // since #139
  )

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("DynamoDbServiceConfigurationRepositorySuite")(
      test("getServiceConfigurations should retrieve nothing when nothing exists") {
        assertZIO(ServiceConfigurationRepository.getServiceConfigurations)(
          equalTo(List.empty[ServiceConfiguration])
        )
      },
      test("getServiceConfiguration should retrieve nothing when nothing exists") {
        assertZIO(ServiceConfigurationRepository.getServiceConfiguration("TestEnv", "NonExistentService").exit)(
          failsWithA[RecordNotFoundDatabaseError]
        )
      },
      test("createNewServiceConfiguration should not fail") {
        assertZIO(ServiceConfigurationRepository.createNewServiceConfiguration(configuration))(isUnit)
      },
      test("getServiceConfigurations should retrieve newly created configuration") {
        assertZIO(ServiceConfigurationRepository.getServiceConfigurations)(
          equalTo(List(configuration))
        )
      },
      test("getServiceConfiguration should retrieve newly created configuration when asked for it") {
        assertZIO(ServiceConfigurationRepository.getServiceConfiguration(configuration.env, configuration.name))(
          equalTo(configuration)
        )
      },
      test("getServiceConfiguration should still retrieve nothing when asked for non-existent service") {
        assertZIO(ServiceConfigurationRepository.getServiceConfiguration("TestEnv", "NonExistentService").exit)(
          failsWithA[RecordNotFoundDatabaseError]
        )
      },
      test("createNewServiceConfiguration should fail when creating already existing service") {
        assertZIO(ServiceConfigurationRepository.createNewServiceConfiguration(configurationUpdated).exit)(
          failsWithA[DataConflictDatabaseError]
        )
      },
      test("getServiceConfigurations should retrieve unchanged configuration") {
        assertZIO(ServiceConfigurationRepository.getServiceConfigurations)(
          equalTo(List(configuration))
        )
      },
      test("createNewServiceConfiguration should not fail when creating another service configuration") {
        assertZIO(ServiceConfigurationRepository.createNewServiceConfiguration(configurationDifferentService))(isUnit)
      },
      test("getServiceConfigurations should retrieve both configurations") {
        assertZIO(ServiceConfigurationRepository.getServiceConfigurations)(
          hasSameElements(List(configuration, configurationDifferentService))
        )
      },
      test("deleteServiceConfiguration should not fail") {
        assertZIO(ServiceConfigurationRepository.deleteServiceConfiguration(configurationDifferentService.env, configurationDifferentService.name))(isUnit)
      },
      test("getServiceConfigurations should retrieve remaining configuration") {
        assertZIO(ServiceConfigurationRepository.getServiceConfigurations)(
          equalTo(List(configuration))
        )
      },
      test("deleteServiceConfiguration should not fail on reentry for now non-existing configuration") {
        assertZIO(ServiceConfigurationRepository.deleteServiceConfiguration(configurationDifferentService.env, configurationDifferentService.name))(isUnit)
      },
      test("getServiceConfigurations should still retrieve the remaining configuration") {
        assertZIO(ServiceConfigurationRepository.getServiceConfigurations)(
          equalTo(List(configuration))
        )
      },
      test("updateExistingServiceConfiguration should not fail") {
        assertZIO(ServiceConfigurationRepository.updateExistingServiceConfiguration(configurationUpdated))(isUnit)
      },
      test("getServiceConfigurations should retrieve updated configuration") {
        assertZIO(ServiceConfigurationRepository.getServiceConfigurations)(
          equalTo(List(configurationUpdated))
        )
      },
      test("updateExistingServiceConfiguration should fail on updating non-exiting configuration") {
        assertZIO(ServiceConfigurationRepository.updateExistingServiceConfiguration(configurationDifferentService).exit)(
          failsWithA[RecordNotFoundDatabaseError]
        )
      },
      test("getServiceConfigurations should retrieve only the last updated configuration") {
        assertZIO(ServiceConfigurationRepository.getServiceConfigurations)(
          equalTo(List(configurationUpdated))
        )
      },
      test("getServiceConfigurations should survive record with no fields introduced after #83") {
        for {
          map <- ZIO.attempt {
            Map(
              "fullServiceName" -> sAttributeValueFromString(s"${legacyConfiguration83.env}|${legacyConfiguration83.name}"),
              "serviceName" -> sAttributeValueFromString(legacyConfiguration83.name),
              // hidden deliberately not present, as records from before #139 don't carry it
              "env" -> sAttributeValueFromString(legacyConfiguration83.env),
              "SnowID" -> sAttributeValueFromString(legacyConfiguration83.snowID),
              "description" -> sAttributeValueFromString(legacyConfiguration83.description),
              "maintenanceMessage" -> sAttributeValueFromString(legacyConfiguration83.maintenanceMessage),
              // linksJson deliberately not present, as records from before #113 don't carry it
              "statusCheckActionJson" -> sAttributeValueFromPOJO(legacyConfiguration83.statusCheckAction),
              "statusCheckIntervalSeconds" -> nAttributeValueFromInt(legacyConfiguration83.statusCheckIntervalSeconds),
              // statusCheckNonGreenIntervalSeconds deliberately not present, as records from before #83 don't carry it
              "notificationConditionJson" -> sAttributeValueFromPOJO(legacyConfiguration83.notificationCondition),
              "notificationActionsJson" -> sAttributeValueFromPOJO(legacyConfiguration83.notificationAction)
            )
          }
          request <- ZIO.config[RepositoriesConfig](RepositoriesConfig.config).map(config =>
            PutItemRequest
              .builder()
              .tableName(config.configurationsTableName)
              .item(map.asJava)
              .build()
          )
          _ <- ZIO.serviceWith[DynamoDbClient](_.putItem(request))
          retrieved <- ServiceConfigurationRepository.getServiceConfigurations
        } yield assert(retrieved.toSet)(equalTo(Set(configurationUpdated, legacyConfiguration83)))
      },
      test("getServiceConfigurations should survive record with no fields introduced after #113") {
        for {
          map <- ZIO.attempt {
            Map(
              "fullServiceName" -> sAttributeValueFromString(s"${legacyConfiguration113.env}|${legacyConfiguration113.name}"),
              "serviceName" -> sAttributeValueFromString(legacyConfiguration113.name),
              // hidden deliberately not present, as records from before #139 don't carry it
              "env" -> sAttributeValueFromString(legacyConfiguration113.env),
              "SnowID" -> sAttributeValueFromString(legacyConfiguration113.snowID),
              "description" -> sAttributeValueFromString(legacyConfiguration113.description),
              "maintenanceMessage" -> sAttributeValueFromString(legacyConfiguration113.maintenanceMessage),
              // linksJson deliberately not present, as records from before #113 don't carry it
              "statusCheckActionJson" -> sAttributeValueFromPOJO(legacyConfiguration113.statusCheckAction),
              "statusCheckIntervalSeconds" -> nAttributeValueFromInt(legacyConfiguration113.statusCheckIntervalSeconds),
              "statusCheckNonGreenIntervalSeconds" -> nAttributeValueFromInt(legacyConfiguration113.statusCheckNonGreenIntervalSeconds),
              "notificationConditionJson" -> sAttributeValueFromPOJO(legacyConfiguration113.notificationCondition),
              "notificationActionsJson" -> sAttributeValueFromPOJO(legacyConfiguration113.notificationAction)
            )
          }
          request <- ZIO.config[RepositoriesConfig](RepositoriesConfig.config).map(config =>
            PutItemRequest
              .builder()
              .tableName(config.configurationsTableName)
              .item(map.asJava)
              .build()
          )
          _ <- ZIO.serviceWith[DynamoDbClient](_.putItem(request))
          retrieved <- ServiceConfigurationRepository.getServiceConfigurations
        } yield assert(retrieved.toSet)(equalTo(Set(configurationUpdated, legacyConfiguration83, legacyConfiguration113)))
      },
      test("getServiceConfigurations should survive record with no fields introduced after #139") {
        for {
          map <- ZIO.attempt {
            Map(
              "fullServiceName" -> sAttributeValueFromString(s"${legacyConfiguration139.env}|${legacyConfiguration139.name}"),
              "serviceName" -> sAttributeValueFromString(legacyConfiguration139.name),
              // hidden deliberately not present, as records from before #139 don't carry it
              "env" -> sAttributeValueFromString(legacyConfiguration139.env),
              "SnowID" -> sAttributeValueFromString(legacyConfiguration139.snowID),
              "description" -> sAttributeValueFromString(legacyConfiguration139.description),
              "maintenanceMessage" -> sAttributeValueFromString(legacyConfiguration139.maintenanceMessage),
              "linksJson" -> sAttributeValueFromPOJO(legacyConfiguration139.links),
              "statusCheckActionJson" -> sAttributeValueFromPOJO(legacyConfiguration139.statusCheckAction),
              "statusCheckIntervalSeconds" -> nAttributeValueFromInt(legacyConfiguration139.statusCheckIntervalSeconds),
              "statusCheckNonGreenIntervalSeconds" -> nAttributeValueFromInt(legacyConfiguration139.statusCheckNonGreenIntervalSeconds),
              "notificationConditionJson" -> sAttributeValueFromPOJO(legacyConfiguration139.notificationCondition),
              "notificationActionsJson" -> sAttributeValueFromPOJO(legacyConfiguration139.notificationAction)
            )
          }
          request <- ZIO.config[RepositoriesConfig](RepositoriesConfig.config).map(config =>
            PutItemRequest
              .builder()
              .tableName(config.configurationsTableName)
              .item(map.asJava)
              .build()
          )
          _ <- ZIO.serviceWith[DynamoDbClient](_.putItem(request))
          retrieved <- ServiceConfigurationRepository.getServiceConfigurations
        } yield assert(retrieved.toSet)(equalTo(Set(configurationUpdated, legacyConfiguration83, legacyConfiguration113, legacyConfiguration139)))
      }
    ) @@ TestAspect.sequential @@ TestAspect.withLiveClock @@ TestAspect.afterAll(deleteTable().ignore)
  }.provide(DynamoDbServiceConfigurationRepository.layer, DynamoDbProvider.layer)

  private def deleteTable(): ZIO[DynamoDbClient, Throwable, Unit] = for {
    tableName <- ZIO.config[RepositoriesConfig](RepositoriesConfig.config).map(_.configurationsTableName)
    _ <- ZIO.serviceWithZIO[DynamoDbClient](_.deleteTableSafe(tableName))
  } yield ()
}
