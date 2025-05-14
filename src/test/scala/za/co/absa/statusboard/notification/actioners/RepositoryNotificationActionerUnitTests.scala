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

package za.co.absa.statusboard.notification.actioners

import org.mockito.Mockito._
import za.co.absa.statusboard.model.NotificationAction
import za.co.absa.statusboard.repository.NotificationRepository
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio.test.Assertion.isUnit
import zio.test.{Spec, TestEnvironment, assertZIO}
import zio.{Scope, ZIO, ZLayer}

object RepositoryNotificationActionerUnitTests extends ConfigProviderSpec {
  private val status = TestData.refinedStatus
  private val action = NotificationAction.Repository()
  private val repositoryMock = mock(classOf[NotificationRepository])

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("RepositoryNotificationActionerSuite")(
      test("Pass status directly to repository") {
        assertZIO(
          for {
            _ <- RepositoryNotificationActioner.notify(action, status)
            _ <- ZIO.attempt(verify(repositoryMock, times(1)).persistNotification(status))
          } yield ()
        )(isUnit)
      }
    )
  }.provide(RepositoryNotificationActionerImpl.layer, ZLayer.succeed(repositoryMock))
}
