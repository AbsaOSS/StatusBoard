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
import software.amazon.awssdk.services.dynamodb.model._
import za.co.absa.statusboard.config.RepositoriesConfig
import za.co.absa.statusboard.model.AppError.DatabaseError
import za.co.absa.statusboard.model.AppError.DatabaseError.GeneralDatabaseError
import za.co.absa.statusboard.model._
import za.co.absa.statusboard.repository.DynamoDbNotificationRepository._
import za.co.absa.statusboard.utils.DynamoDbUtils.{nAttributeValueFromLong, sAttributeValueFromString, _}
import zio._

import java.time.Instant
import java.util
import scala.jdk.CollectionConverters._

class DynamoDbNotificationRepository(dynamodbClient: DynamoDbClient, tableName: String) extends NotificationRepository {
  override def persistNotification(status: RefinedStatus): IO[DatabaseError, Unit] = {
    for {
      map <- mapFromStatus(status)
      request <- ZIO.attempt {
        PutItemRequest.builder().tableName(tableName).item(map.asJava).build()
      }
      _ <- ZIO.attempt(dynamodbClient.putItem(request))
    } yield ()
  }.catchAll { error =>
    ZIO.logError(s"An error occurred while persisting notification: ${error.getMessage}") *>
      ZIO.fail(GeneralDatabaseError(s"An error occurred while persisting notification:. ${error.getMessage}"))
  }

  override def renameService(environmentOld: String, serviceNameOld: String, environmentNew: String, serviceNameNew: String): IO[DatabaseError, Unit] = {
    for {
      oldRecordsRequest <- ZIO.attempt {
        QueryRequest
          .builder()
          .tableName(tableName)
          .keyConditionExpression(s"$FullServiceName = :$FullServiceName")
          .expressionAttributeValues(Map(s":$FullServiceName" -> sAttributeValueFromString(fullServiceName(environmentOld, serviceNameOld))).asJava)
          .build()
      }
      oldRecordsRequestResponse <- ZIO.attempt(dynamodbClient.query(oldRecordsRequest))
      oldRecords <- ZIO.attempt(oldRecordsRequestResponse.items().asScala.toSeq)
      newRecords <- ZIO.succeed {
        oldRecords.map { map: util.Map[String, AttributeValue] =>
          map.asScala.map {
            case (FullServiceName, _) => FullServiceName -> sAttributeValueFromString(fullServiceName(environmentNew, serviceNameNew))
            case (ServiceName, _) => ServiceName -> sAttributeValueFromString(serviceNameNew)
            case (Environment, _) => Environment -> sAttributeValueFromString(environmentNew)
            case other => other
          }.asJava
        }
      }
      // FIRST: make records under new name
      _ <- ZIO.foreachDiscard(newRecords)(newRecord => for {
        request <- ZIO.attempt {
          PutItemRequest.builder().tableName(tableName).item(newRecord).build()
        }
        _ <- ZIO.attempt(dynamodbClient.putItem(request))
      } yield ())
      // SECOND: remove records under old name
      _ <- ZIO.foreachDiscard(oldRecords)(oldRecord => for {
        deleteRequest <- ZIO.attempt {
          DeleteItemRequest
            .builder()
            .tableName(tableName)
            .key(Map(
              FullServiceName -> sAttributeValueFromString(fullServiceName(environmentOld, serviceNameOld)),
              NotificationTime -> oldRecord.get(NotificationTime)
            ).asJava)
            .build()
        }
        _ <- ZIO.logInfo(deleteRequest.toString)
        _ <- ZIO.attempt(dynamodbClient.deleteItem(deleteRequest))
      } yield ())
    } yield ()
  }.catchAll { error =>
    val msg = s"An error occurred while renaming $environmentOld $serviceNameOld to $environmentNew $serviceNameNew"
    ZIO.logError(s"$msg: ${error.getMessage}") *> ZIO.fail(GeneralDatabaseError(s"$msg: ${error.getMessage}"))
  }
}

object DynamoDbNotificationRepository {
  private val FullServiceName = "fullServiceName" // (Environment | ServiceName) PartitionKey
  private val ServiceName = "serviceName"
  private val Environment = "env"
  private val NotificationTime = "notificationTime" // SortKey
  private val Status = "status"
  private val MaintenanceMessage = "maintenanceMessage"
  private val FirstSeen = "firstSeen"
  private val LastSeen = "lastSeen"

  private def fullServiceName(environment: String, serviceName: String): String = s"$environment|$serviceName"

  private def mapFromStatus(status: RefinedStatus): Task[Map[String, AttributeValue]] = {
    val notificationTime = Instant.now()
    ZIO.attempt {
      Map(
        FullServiceName -> sAttributeValueFromString(fullServiceName(status.env, status.serviceName)),
        ServiceName -> sAttributeValueFromString(status.serviceName),
        Environment -> sAttributeValueFromString(status.env),
        NotificationTime -> nAttributeValueFromLong(notificationTime.toEpochMilli),
        Status -> sAttributeValueFromPOJO(status.status),
        MaintenanceMessage -> sAttributeValueFromString(status.maintenanceMessage),
        FirstSeen -> nAttributeValueFromLong(status.firstSeen.toEpochMilli),
        LastSeen -> nAttributeValueFromLong(status.lastSeen.toEpochMilli)
      )
    }
  }

  private def createTableIfNotExists(dynamoDbClient: DynamoDbClient, tableName: String): Task[Unit] = for {
    request <- ZIO.attempt {
      CreateTableRequest
        .builder()
        .tableName(tableName)
        .attributeDefinitions(
          AttributeDefinition.builder().attributeName(FullServiceName).attributeType(ScalarAttributeType.S).build(),
          AttributeDefinition.builder().attributeName(NotificationTime).attributeType(ScalarAttributeType.N).build()
        )
        .keySchema(
          KeySchemaElement.builder().attributeName(FullServiceName).keyType(KeyType.HASH).build(),
          KeySchemaElement.builder().attributeName(NotificationTime).keyType(KeyType.RANGE).build()
        )
        .provisionedThroughput(
          ProvisionedThroughput.builder().readCapacityUnits(10L).writeCapacityUnits(5L).build()
        )
        .build()
    }
    _ <- dynamoDbClient.createTableSafe(request)
  } yield ()

  val layer: RLayer[DynamoDbClient, NotificationRepository] = ZLayer {
    for {
      tableName <- ZIO.config[RepositoriesConfig](RepositoriesConfig.config).map(_.notificationsTableName)
      dynamodbClient <- ZIO.service[DynamoDbClient]
      _ <- createTableIfNotExists(dynamodbClient, tableName)
    } yield new DynamoDbNotificationRepository(dynamodbClient, tableName)
  }
}
