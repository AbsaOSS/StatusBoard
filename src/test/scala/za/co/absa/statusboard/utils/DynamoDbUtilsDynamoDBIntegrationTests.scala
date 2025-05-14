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

package za.co.absa.statusboard.utils

import io.circe.syntax.EncoderOps
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model._
import za.co.absa.statusboard.model.RawStatus
import za.co.absa.statusboard.providers.DynamoDbProvider
import za.co.absa.statusboard.testUtils.ConfigProviderSpec
import za.co.absa.statusboard.utils.DynamoDbUtils._
import zio._
import zio.test.Assertion.{equalTo, isUnit}
import zio.test._

import java.util.UUID
import scala.jdk.CollectionConverters.MapHasAsJava

object DynamoDbUtilsDynamoDBIntegrationTests extends ConfigProviderSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("DynamoDbUtilsSuite")(
      test("creating table should succeed") {
        for {
          request <- ZIO.attempt {
            CreateTableRequest
              .builder()
              .tableName(testTableName)
              .attributeDefinitions(
                AttributeDefinition
                  .builder()
                  .attributeName("uuid")
                  .attributeType(ScalarAttributeType.S)
                  .build()
              )
              .keySchema(
                KeySchemaElement.builder().attributeName("uuid").keyType(KeyType.HASH).build()
              )
              .provisionedThroughput(
                ProvisionedThroughput.builder().readCapacityUnits(10L).writeCapacityUnits(5L).build()
              )
              .build()
          }
          _ <- ZIO.serviceWithZIO[DynamoDbClient](_.createTableSafe(request))
        } yield assertCompletes
      },
      test("storing record with all attributes should not fail") {
        for {
          map <- ZIO.attempt {
            Map(
              "pojo" -> sAttributeValueFromPOJO(RawStatus.Red("TestPojo", intermittent = false): RawStatus),
              "json" -> sAttributeValueFromJson((RawStatus.Red("TestJson", intermittent = true): RawStatus).asJson),
              "string" -> sAttributeValueFromString("TestString"),
              "uuid" -> sAttributeValueFromUUID(UUID.fromString("01234567-89ab-cdef-fedc-ba9876543210")),
              "long" -> nAttributeValueFromLong(444333222111L),
              "int" -> nAttributeValueFromInt(333222111),
              "bool" -> boolAttributeValueFromBoolean(false),
              "stringSet" -> ssAttributeValueFromStringSet(Set("set", "has", "no", "order"))
            )
          }
          request <- ZIO.attempt(PutItemRequest.builder().tableName(testTableName).item(map.asJava).build())
          _ <- ZIO.serviceWith[DynamoDbClient](_.putItem(request))
        } yield assertCompletes
      },
      test("reading record should carry same attributes as ones stored") {
        for {
          request <- ZIO.attempt(ScanRequest.builder().tableName(testTableName).build())
          response <- ZIO.serviceWith[DynamoDbClient](_.scan(request))
          item <- ZIO.attempt(response.items().get(0))
          _ <- assertZIO(sAttributeValueToPOJO[RawStatus](item, "pojo"))(equalTo(RawStatus.Red("TestPojo", intermittent = false): RawStatus))
          _ <- assertZIO(sAttributeValueToJson(item, "json"))(equalTo((RawStatus.Red("TestJson", intermittent = true): RawStatus).asJson))
          _ <- assert(sAttributeValueToString(item, "string"))(equalTo("TestString"))
          _ <- assert(sAttributeValueToUUID(item, "uuid"))(equalTo(UUID.fromString("01234567-89ab-cdef-fedc-ba9876543210")))
          _ <- assert(nAttributeValueToLong(item, "long"))(equalTo(444333222111L))
          _ <- assert(nAttributeValueToInt(item, "int"))(equalTo(333222111))
          _ <- assert(boolAttributeValueToBoolean(item, "bool"))(equalTo(false))
          _ <- assert(ssAttributeValueToStringSet(item, "stringSet"))(equalTo(Set("set", "has", "no", "order")))
        } yield assertCompletes
      },
      test("deleting table should succeed") {
        assertZIO(ZIO.serviceWithZIO[DynamoDbClient](_.deleteTableSafe(testTableName)))(isUnit)
      }
    ) @@ TestAspect.sequential @@ TestAspect.withLiveClock
  }.provide(DynamoDbProvider.layer)

  private val testTableName = s"integration-test-attribute-table-${UUID.randomUUID}"
}
