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
import za.co.absa.statusboard.model.AppError.DatabaseError.{GeneralDatabaseError, RecordNotFoundDatabaseError}
import za.co.absa.statusboard.model.{RawStatus, RefinedStatus}
import za.co.absa.statusboard.repository.DynamoDbStatusRepository.{fullServiceName, _}
import za.co.absa.statusboard.utils.DynamoDbUtils.{DynamoDbExtensions, boolAttributeValueFromBoolean, boolAttributeValueToBoolean, nAttributeValueFromLong, nAttributeValueToLong, sAttributeValueFromPOJO, sAttributeValueFromString, sAttributeValueToPOJO, sAttributeValueToString}
import zio._

import java.time.Instant
import scala.jdk.CollectionConverters._

class DynamoDbStatusRepository(dynamodbClient: DynamoDbClient, tableName: String) extends StatusRepository {
  override def createOrUpdate(status: RefinedStatus): IO[DatabaseError, Unit] = {
    for {
      map <- mapFromStatus(status)
      request <- ZIO.attempt {
        PutItemRequest.builder().tableName(tableName).item(map.asJava).build()
      }
      _ <- ZIO.attempt(dynamodbClient.putItem(request))
    } yield ()
  }.catchAll { error =>
    val msg = "An error occurred while persisting notification"
    ZIO.logError(s"$msg: ${error.getMessage}") *> ZIO.fail(GeneralDatabaseError(s"$msg: ${error.getMessage}"))
  }

  override def getLatestStatus(environment: String, serviceName: String): IO[DatabaseError, RefinedStatus] = getLatestStatus(fullServiceName(environment, serviceName), onlyNotified = false)

  override def getLatestNotifiedStatus(environment: String, serviceName: String): IO[DatabaseError, RefinedStatus] = getLatestStatus(fullServiceName(environment, serviceName), onlyNotified = true)

  override def getAllStatuses(environment: String, serviceName: String): IO[DatabaseError, Seq[RefinedStatus]] = {
    for {
      request <- ZIO.attempt {
        QueryRequest
          .builder()
          .tableName(tableName)
          .keyConditionExpression(s"$FullServiceName = :$FullServiceName")
          .expressionAttributeValues(Map(s":$FullServiceName" -> sAttributeValueFromString(fullServiceName(environment, serviceName))).asJava)
          .build()
      }
      response <- ZIO.attempt(dynamodbClient.query(request))
      responseItems <- ZIO.attempt(response.items().asScala.toSeq)
      serviceConfigurations <- ZIO.foreach(responseItems)(mapToStatus)
    } yield serviceConfigurations
  }.catchAll { error =>
    val msg = "An error occurred while retrieving full service status history"
    ZIO.logError(s"$msg: ${error.getMessage}") *> ZIO.fail(GeneralDatabaseError(s"$msg: ${error.getMessage}"))
  }

  override def getLatestStatusOfAllActiveConfigurations(): IO[DatabaseError, Set[RefinedStatus]] = {
    for {
      scanRequest <- ZIO.attempt(ScanRequest.builder().tableName(tableName).attributesToGet(FullServiceName).build())
      scanResponse <- ZIO.attempt(dynamodbClient.scan(scanRequest))
      scanResponseItems <- ZIO.attempt(scanResponse.items().asScala.toSeq)
      fullServiceNames <- ZIO.foreach(scanResponseItems)(item => ZIO.succeed(sAttributeValueToString(item, FullServiceName)))
      result <- ZIO.foreach(fullServiceNames.toSet)(fullServiceName => getLatestStatus(fullServiceName, onlyNotified = false))
    } yield result.filter(_.status match {
      case RawStatus.Black() => false
      case _ => true
    })
  }.catchAll { error =>
    val msg = "An error occurred while retrieving status of all services"
    ZIO.logError(s"$msg: ${error.getMessage}") *> ZIO.fail(GeneralDatabaseError(s"$msg: ${error.getMessage}"))
  }

  override def renameService(environmentOld: String, serviceNameOld: String, environmentNew: String, serviceNameNew: String): IO[DatabaseError, Unit] = {
    for {
      oldStatuses <- getAllStatuses(environmentOld, serviceNameOld)
      newStatuses <- ZIO.succeed {
        oldStatuses.map(_.copy(
          env = environmentNew,
          serviceName = serviceNameNew,
        ))
      }
      // FIRST: make records under new name
      _ <- ZIO.foreachDiscard(newStatuses)(newStatus => createOrUpdate(newStatus))
      // SECOND: remove records under old name
      _ <- ZIO.foreachDiscard(oldStatuses)(oldStatus => for {
        deleteRequest <- ZIO.attempt {
          DeleteItemRequest
            .builder()
            .tableName(tableName)
            .key(Map(
              FullServiceName -> sAttributeValueFromString(fullServiceName(environmentOld, serviceNameOld)),
              FirstSeen -> nAttributeValueFromLong(oldStatus.firstSeen.toEpochMilli)
            ).asJava)
            .build()
        }
        _ <- ZIO.attempt(dynamodbClient.deleteItem(deleteRequest))
      } yield ())
    } yield ()
  }.catchAll { error =>
    val msg = s"An error occurred while renaming $environmentOld $serviceNameOld to $environmentNew $serviceNameNew"
    ZIO.logError(s"$msg: ${error.getMessage}") *> ZIO.fail(GeneralDatabaseError(s"$msg: ${error.getMessage}"))
  }

  private def getLatestStatus(fullServiceName: String, onlyNotified: Boolean): IO[DatabaseError, RefinedStatus] = {
    for {
      request <- ZIO.attempt {
        val expressionValues = if (onlyNotified)
          Map(
            s":$FullServiceName" -> sAttributeValueFromString(fullServiceName),
            s":$NotificationSent" -> boolAttributeValueFromBoolean(true)
          )
        else
          Map(
            s":$FullServiceName" -> sAttributeValueFromString(fullServiceName)
          )

        val baseBuilder = QueryRequest
          .builder()
          .tableName(tableName)
          .keyConditionExpression(s"$FullServiceName = :$FullServiceName")
          .expressionAttributeValues(expressionValues.asJava)

        val maybeFilteredBuilder = if (onlyNotified)
          baseBuilder.filterExpression(s"$NotificationSent = :$NotificationSent")
        else
          baseBuilder

        maybeFilteredBuilder.scanIndexForward(false)
          // no limit - as it would be applied before the filter
          .build()
      }
      response <- ZIO.attempt(dynamodbClient.query(request))
      serviceConfiguration <-
        if (response.items().isEmpty)
          ZIO.fail(RecordNotFoundDatabaseError(s"No record found for service: $fullServiceName"))
        else
          mapToStatus(response.items().get(0))
    } yield serviceConfiguration
  }.catchAll {
    case error: RecordNotFoundDatabaseError =>
      ZIO.logInfo(error.message) *>
        ZIO.fail(error)
    case error =>
      val msg = "An error occurred while retrieving service status"
      ZIO.logError(s"$msg: ${error.getMessage}") *> ZIO.fail(GeneralDatabaseError(s"$msg: ${error.getMessage}"))
  }
}

object DynamoDbStatusRepository {
  private val FullServiceName = "fullServiceName" // (Environment | ServiceName) PartitionKey
  private val ServiceName = "serviceName"
  private val Environment = "env"
  private val Status = "status"
  private val MaintenanceMessage = "maintenanceMessage"
  private val FirstSeen = "firstSeen" // SortKey
  private val LastSeen = "lastSeen"
  private val NotificationSent = "notificationSent"

  private def fullServiceName(environment: String, serviceName: String): String = s"$environment|$serviceName"
  private def fullServiceName(status: RefinedStatus): String = fullServiceName(status.env, status.serviceName)

  private def mapToStatus(item: java.util.Map[String, AttributeValue]): IO[circe.Error, RefinedStatus] = {
    for {
      status <- sAttributeValueToPOJO[RawStatus](item, Status)
    }
    yield RefinedStatus(
      serviceName = sAttributeValueToString(item, ServiceName),
      env = sAttributeValueToString(item, Environment),
      status = status,
      maintenanceMessage = sAttributeValueToString(item, MaintenanceMessage),
      firstSeen = Instant.ofEpochMilli(nAttributeValueToLong(item, FirstSeen)),
      lastSeen = Instant.ofEpochMilli(nAttributeValueToLong(item, LastSeen)),
      notificationSent = boolAttributeValueToBoolean(item, NotificationSent)
    )
  }

  private def mapFromStatus(status: RefinedStatus): Task[Map[String, AttributeValue]] = {
    ZIO.attempt {
      Map(
        FullServiceName -> sAttributeValueFromString(fullServiceName(status)),
        ServiceName -> sAttributeValueFromString(status.serviceName),
        Environment -> sAttributeValueFromString(status.env),
        Status -> sAttributeValueFromPOJO(status.status),
        MaintenanceMessage -> sAttributeValueFromString(status.maintenanceMessage),
        FirstSeen -> nAttributeValueFromLong(status.firstSeen.toEpochMilli),
        LastSeen -> nAttributeValueFromLong(status.lastSeen.toEpochMilli),
        NotificationSent -> boolAttributeValueFromBoolean(status.notificationSent)
      )
    }
  }

  private def createTableIfNotExists(dynamoDbClient: DynamoDbClient, tableName: String): Task[Unit] = for {
    request <- ZIO.attempt {
      CreateTableRequest
        .builder()
        .tableName(tableName)
        .attributeDefinitions(
          AttributeDefinition
            .builder()
            .attributeName("fullServiceName")
            .attributeType(ScalarAttributeType.S)
            .build(),
          AttributeDefinition
            .builder()
            .attributeName("firstSeen")
            .attributeType(ScalarAttributeType.N)
            .build()
        )
        .keySchema(
          KeySchemaElement.builder().attributeName("fullServiceName").keyType(KeyType.HASH).build(),
          KeySchemaElement.builder().attributeName("firstSeen").keyType(KeyType.RANGE).build()
        )
        .provisionedThroughput(
          ProvisionedThroughput.builder().readCapacityUnits(10L).writeCapacityUnits(5L).build()
        )
        .build()
    }
    _ <- dynamoDbClient.createTableSafe(request)
  } yield ()

  val layer: RLayer[DynamoDbClient, StatusRepository] = ZLayer {
    for {
      tableName <- ZIO.config[RepositoriesConfig](RepositoriesConfig.config).map(_.statusesTableName)
      dynamodbClient <- ZIO.service[DynamoDbClient]
      _ <- createTableIfNotExists(dynamodbClient, tableName)
    } yield new DynamoDbStatusRepository(dynamodbClient, tableName)
  }
}
