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

import io.circe
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model._
import za.co.absa.statusboard.config.RepositoriesConfig
import za.co.absa.statusboard.model.AppError.DatabaseError
import za.co.absa.statusboard.model.AppError.DatabaseError._
import za.co.absa.statusboard.model.ServiceConfiguration.ServiceConfigurationLinks
import za.co.absa.statusboard.model.{NotificationAction, NotificationCondition, ServiceConfiguration, StatusCheckAction}
import za.co.absa.statusboard.repository.DynamoDbServiceConfigurationRepository._
import za.co.absa.statusboard.utils.DynamoDbUtils._
import zio._

import scala.jdk.CollectionConverters._

class DynamoDbServiceConfigurationRepository(dynamodbClient: DynamoDbClient, tableName: String) extends ServiceConfigurationRepository {
  override def getServiceConfigurations: IO[DatabaseError, Seq[ServiceConfiguration]] = {
    for {
      request <- ZIO.attempt(ScanRequest.builder().tableName(tableName).build())
      response <- ZIO.attempt(dynamodbClient.scan(request))
      responseItems <- ZIO.attempt(response.items().asScala.toSeq)
      serviceConfigurations <- ZIO.foreach(responseItems)(mapToServiceConfiguration)
    } yield serviceConfigurations
  }.catchAll { error =>
    ZIO.logError(s"An error occurred while scanning the database: ${error.getMessage}") *>
      ZIO.fail(GeneralDatabaseError(s"An error occurred while retrieving http services."))
  }

  override def getServiceConfiguration(environment: String, serviceName: String): IO[DatabaseError, ServiceConfiguration] = {
    for {
      request <- ZIO.attempt {
        GetItemRequest
          .builder()
          .tableName(tableName)
          .key(Map(FullServiceName -> sAttributeValueFromString(fullServiceName(environment, serviceName))).asJava)
          .build()
      }
      response <- ZIO.attempt(dynamodbClient.getItem(request))
      serviceConfiguration <-
        if (response.hasItem)
          mapToServiceConfiguration(response.item())
        else
          ZIO.fail(RecordNotFoundDatabaseError(s"No record found for service: ${fullServiceName(environment, serviceName)}"))
    } yield serviceConfiguration
  }.catchAll {
    case error: RecordNotFoundDatabaseError =>
      ZIO.logError(error.message) *>
        ZIO.fail(error)
    case error =>
      ZIO.logError(s"An error occurred while retrieving service configurations: ${error.getMessage}") *>
        ZIO.fail(GeneralDatabaseError(s"An error occurred while retrieving service configurations: ${error.getMessage}"))
  }

  override def createNewServiceConfiguration(configuration: ServiceConfiguration): IO[DatabaseError, Unit] = {
    for {
      map <- mapFromServiceConfiguration(configuration)
      request <- ZIO.attempt {
        PutItemRequest
          .builder()
          .tableName(tableName)
          .item(map.asJava)
          .conditionExpression(s"attribute_not_exists($FullServiceName)")
          .build()
      }
      _ <- ZIO.attempt(dynamodbClient.putItem(request))
      _ <- ZIO.logInfo(s"Created new configuration ${fullServiceName(configuration)}")
    } yield ()
  }.catchAll {
    case error: ConditionalCheckFailedException =>
      ZIO.logError(s"Record already exists: ${error.getMessage}") *>
        ZIO.fail(DataConflictDatabaseError(s"Record already exists"))
    case error =>
      ZIO.logError(s"An error occurred while creating service record: ${error.getMessage}") *>
        ZIO.fail(GeneralDatabaseError(s"An error occurred while creating service record: ${error.getMessage}"))
  }

  override def updateExistingServiceConfiguration(configuration: ServiceConfiguration): IO[DatabaseError, Unit] = {
    for {
      map <- mapFromServiceConfiguration(configuration)
      request <- ZIO.attempt {
        PutItemRequest
          .builder()
          .tableName(tableName)
          .item(map.asJava)
          .conditionExpression(s"attribute_exists($FullServiceName)")
          .build()
      }
      _ <- ZIO.attempt(dynamodbClient.putItem(request))
      _ <- ZIO.logInfo(s"Updated existing configuration ${fullServiceName(configuration)}")
    } yield ()
  }.catchAll {
    case error: ConditionalCheckFailedException =>
      ZIO.logError(s"Record does not exist: ${error.getMessage}") *>
        ZIO.fail(RecordNotFoundDatabaseError(s"Record does not exist: ${error.getMessage}"))
    case error =>
      ZIO.logError(s"An error occurred while updating service record: ${error.getMessage}") *>
        ZIO.fail(GeneralDatabaseError("An error occurred while updating service record."))
  }

  override def deleteServiceConfiguration(environment: String, serviceName: String): IO[DatabaseError, Unit] = {
    for {
      request <- ZIO.attempt {
        DeleteItemRequest
          .builder()
          .tableName(tableName)
          .key(Map(FullServiceName -> sAttributeValueFromString(fullServiceName(environment, serviceName))).asJava)
          .build()
      }
      _ <- ZIO.attempt(dynamodbClient.deleteItem(request))
      _ <- ZIO.logInfo(s"Deleted configuration ${fullServiceName(environment, serviceName)}")
    } yield ()
  }.catchAll { error =>
    ZIO.logError(s"An error occurred while deleting service record: ${error.getMessage}") *>
      ZIO.fail(GeneralDatabaseError("An error occurred while deleting service record."))
  }
}

object DynamoDbServiceConfigurationRepository {
  private val FullServiceName = "fullServiceName" // (Environment | ServiceName) PartitionKey part == Unique Key
  private val ServiceName = "serviceName"
  private val Environment = "env"
  private val Hidden = "hidden"
  private val SnowID = "SnowID"
  private val Description = "description"
  private val MaintenanceMessage = "maintenanceMessage"
  private val LinksJson = "linksJson"
  private val StatusCheckActionJson = "statusCheckActionJson"
  private val StatusCheckIntervalSeconds = "statusCheckIntervalSeconds"
  private val StatusCheckNonGreenIntervalSeconds = "statusCheckNonGreenIntervalSeconds"
  private val NotificationConditionJson = "notificationConditionJson"
  private val NotificationActionsJson = "notificationActionsJson"

  private def fullServiceName(environment: String, serviceName: String): String = s"$environment|$serviceName"
  private def fullServiceName(configuration: ServiceConfiguration): String = fullServiceName(configuration.env, configuration.name)

  private def mapToServiceConfiguration(item: java.util.Map[String, AttributeValue]): IO[circe.Error, ServiceConfiguration] = {
    for {
      statusCheckAction <- sAttributeValueToPOJO[StatusCheckAction](item, StatusCheckActionJson)
      notificationCondition <- sAttributeValueToPOJO[NotificationCondition](item, NotificationConditionJson)
      notificationActions <- sAttributeValueToPOJO[Seq[NotificationAction]](item, NotificationActionsJson)
      links <- if (item.containsKey(LinksJson))
        sAttributeValueToPOJO[ServiceConfigurationLinks](item, LinksJson)
      else
        ZIO.succeed(ServiceConfigurationLinks("", "", "", "", ""))
    }
    yield ServiceConfiguration(
      name = sAttributeValueToString(item, ServiceName),
      env = sAttributeValueToString(item, Environment),
      hidden = if (item.containsKey(Hidden)) boolAttributeValueToBoolean(item, Hidden) else false,
      snowID = sAttributeValueToString(item, SnowID),
      description = sAttributeValueToString(item, Description),
      maintenanceMessage = sAttributeValueToString(item, MaintenanceMessage),
      links = links,
      statusCheckAction = statusCheckAction,
      statusCheckIntervalSeconds = nAttributeValueToInt(item, StatusCheckIntervalSeconds),
      statusCheckNonGreenIntervalSeconds = if (item.containsKey(StatusCheckNonGreenIntervalSeconds)) nAttributeValueToInt(item, StatusCheckNonGreenIntervalSeconds) else nAttributeValueToInt(item, StatusCheckIntervalSeconds),
      notificationCondition = notificationCondition,
      notificationAction = notificationActions
    )
  }

  private def mapFromServiceConfiguration(configuration: ServiceConfiguration): Task[Map[String, AttributeValue]] = {
    ZIO.attempt {
      Map(
        FullServiceName -> sAttributeValueFromString(fullServiceName(configuration)),
        ServiceName -> sAttributeValueFromString(configuration.name),
        Environment -> sAttributeValueFromString(configuration.env),
        Hidden -> boolAttributeValueFromBoolean(configuration.hidden),
        SnowID -> sAttributeValueFromString(configuration.snowID),
        Description -> sAttributeValueFromString(configuration.description),
        MaintenanceMessage -> sAttributeValueFromString(configuration.maintenanceMessage),
        LinksJson -> sAttributeValueFromPOJO(configuration.links),
        StatusCheckActionJson -> sAttributeValueFromPOJO(configuration.statusCheckAction),
        StatusCheckIntervalSeconds -> nAttributeValueFromInt(configuration.statusCheckIntervalSeconds),
        StatusCheckNonGreenIntervalSeconds -> nAttributeValueFromInt(configuration.statusCheckNonGreenIntervalSeconds),
        NotificationConditionJson -> sAttributeValueFromPOJO(configuration.notificationCondition),
        NotificationActionsJson -> sAttributeValueFromPOJO(configuration.notificationAction)
      )
    }
  }

  private def createTableIfNotExists(dynamoDbClient: DynamoDbClient, tableName: String): Task[Unit] = for {
    request <- ZIO.attempt {
      CreateTableRequest
        .builder()
        .tableName(tableName)
        .attributeDefinitions(
          AttributeDefinition.builder().attributeName("fullServiceName").attributeType(ScalarAttributeType.S).build()
        )
        .keySchema(
          KeySchemaElement.builder().attributeName("fullServiceName").keyType(KeyType.HASH).build()
        )
        .provisionedThroughput(
          ProvisionedThroughput.builder().readCapacityUnits(10L).writeCapacityUnits(5L).build()
        )
        .build()
    }
    _ <- dynamoDbClient.createTableSafe(request)
  } yield ()

  val layer: RLayer[DynamoDbClient, ServiceConfigurationRepository] = ZLayer {
    for {
      tableName <-ZIO.config[RepositoriesConfig](RepositoriesConfig.config).map(_.configurationsTableName)
      dynamodbClient <- ZIO.service[DynamoDbClient]
      _ <- createTableIfNotExists(dynamodbClient, tableName)
    } yield new DynamoDbServiceConfigurationRepository(dynamodbClient, tableName)
  }
}
