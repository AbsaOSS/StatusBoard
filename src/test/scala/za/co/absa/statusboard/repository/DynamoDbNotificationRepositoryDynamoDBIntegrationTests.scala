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
import za.co.absa.statusboard.config.RepositoriesConfig
import za.co.absa.statusboard.providers.DynamoDbProvider
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import za.co.absa.statusboard.utils.DynamoDbUtils.DynamoDbExtensions
import zio._
import zio.test.Assertion.isUnit
import zio.test._

object DynamoDbNotificationRepositoryDynamoDBIntegrationTests extends ConfigProviderSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("DynamoDbNotificationRepositorySuite")(
      test("persistNotification should not fail with valid input") {
        assertZIO(NotificationRepository.persistNotification(TestData.refinedStatus))(isUnit)
      },
      test("renameService should not fail with valid input") {
        assertZIO(NotificationRepository.renameService(TestData.refinedStatus.env, TestData.refinedStatus.serviceName, "SomeOtherEnv", "SomeOtherService"))(isUnit)
      }
    ) @@ TestAspect.withLiveClock @@ TestAspect.afterAll(deleteTable().ignore)
  }.provide(DynamoDbNotificationRepository.layer, DynamoDbProvider.layer)

  private def deleteTable(): ZIO[DynamoDbClient, Throwable, Unit] = for {
    tableName <- ZIO.config[RepositoriesConfig](RepositoriesConfig.config).map(_.notificationsTableName)
    _ <- ZIO.serviceWithZIO[DynamoDbClient](_.deleteTableSafe(tableName))
  } yield ()
}
