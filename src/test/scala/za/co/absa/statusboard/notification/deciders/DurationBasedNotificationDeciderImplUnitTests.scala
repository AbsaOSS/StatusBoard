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

package za.co.absa.statusboard.notification.deciders

import org.mockito.Mockito.{mock, reset, when}
import za.co.absa.statusboard.model.AppError.DatabaseError.RecordNotFoundDatabaseError
import za.co.absa.statusboard.model.{NotificationCondition, RefinedStatus}
import za.co.absa.statusboard.model.RawStatus.{Green, Red}
import za.co.absa.statusboard.repository.StatusRepository
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio.test.Assertion.equalTo
import zio.test.{Spec, TestAspect, TestEnvironment, assert}
import zio.{Scope, Task, ZIO, ZLayer}

import java.time.Duration

object DurationBasedNotificationDeciderImplUnitTests extends ConfigProviderSpec {
  private def statusGreen(sent: Boolean, seen: Int) = TestData.refinedStatus.copy(
      status = Green("INFO"),
      notificationSent = sent,
      lastSeen = TestData.refinedStatus.firstSeen.plus(Duration.ofMinutes(seen))
    )

  private def statusRed(sent: Boolean, seen: Int) = TestData.refinedStatus.copy(
    status = Red("INFO", intermittent = false),
    notificationSent = sent,
    lastSeen = TestData.refinedStatus.firstSeen.plus(Duration.ofMinutes(seen))
  )

  private def statusRedInt(sent: Boolean, seen: Int) = TestData.refinedStatus.copy(
    status = Red("INFO", intermittent = true),
    notificationSent = sent,
    lastSeen = TestData.refinedStatus.firstSeen.plus(Duration.ofMinutes(seen))
  )

  private def condition(seen: Int) = NotificationCondition.DurationBased(Duration.ofMinutes(seen).toSeconds.toInt)

  private val repositoryMock = mock(classOf[StatusRepository])

  private def setupRepoMock(lastNotifiedStatus: RefinedStatus): Task[Unit] = ZIO.attempt {
    reset(repositoryMock)
    when(repositoryMock.getLatestNotifiedStatus(TestData.refinedStatus.env, TestData.refinedStatus.serviceName))
      .thenReturn(ZIO.succeed(lastNotifiedStatus))
  }

  private def setupRepoMockEmpty(): Task[Unit] = ZIO.attempt {
    reset(repositoryMock)
    when(repositoryMock.getLatestNotifiedStatus(TestData.refinedStatus.env, TestData.refinedStatus.serviceName))
      .thenReturn(ZIO.fail(RecordNotFoundDatabaseError("BAKA")))
  }

  private def resetRepoMock(): Task[Unit] = ZIO.attempt {
    reset(repositoryMock)
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("DurationBasedNotificationDeciderImplSuite")(
      test("Should yes request notification, when being stable (regardless of no overtime) and previously notified for different color") {
        for {
          _ <- setupRepoMock(statusGreen(sent = true, seen = 10))
          response <- DurationBasedNotificationDecider.shouldNotify(condition(seen = 10), statusRed(sent = false, seen = 5))
        } yield assert(response)(equalTo(true))
      },
      test("Should yes request notification, when being stable (regardless of no overtime) and previously no notification at all") {
        for {
          _ <- setupRepoMockEmpty()
          response <- DurationBasedNotificationDecider.shouldNotify(condition(seen = 10), statusRed(sent = false, seen = 5))
        } yield assert(response)(equalTo(true))
      },
      test("Should yes request notification, when being intermittent, with overtime and previously notified for different color") {
        for {
          _ <- setupRepoMock(statusGreen(sent = true, seen = 10))
          response <- DurationBasedNotificationDecider.shouldNotify(condition(seen = 5), statusRedInt(sent = false, seen = 10))
        } yield assert(response)(equalTo(true))
      },
      test("Should yes request notification, when being green (regardless of no overtime) and previously notified for different color (#160 negated in #5)") {
        for {
          _ <- setupRepoMock(statusRed(sent = true, seen = 10))
          response <- DurationBasedNotificationDecider.shouldNotify(condition(seen = 10), statusGreen(sent = false, seen = 5))
        } yield assert(response)(equalTo(true))
      },
      test("Should not request notification, when already notified - also should not touch repo") {
        for {
          _ <- resetRepoMock()
          response <- DurationBasedNotificationDecider.shouldNotify(condition(seen = 10), statusRed(sent = true, seen = 5))
        } yield assert(response)(equalTo(false))
      },
      test("Should not request notification, when being intermittent with no overtime - also should not touch repo") {
        for {
          _ <- resetRepoMock()
          response <- DurationBasedNotificationDecider.shouldNotify(condition(seen = 10), statusRedInt(sent = false, seen = 5))
        } yield assert(response)(equalTo(false))
      },
      test("Should not request notification, when previously notified for same color") {
        for {
          _ <- setupRepoMock(statusRed(sent = true, seen = 10))
          response <- DurationBasedNotificationDecider.shouldNotify(condition(seen = 10), statusRed(sent = false, seen = 5))
        } yield assert(response)(equalTo(false))
      },
    ) @@ TestAspect.sequential
  }.provide(
    DurationBasedNotificationDeciderImpl.layer,
    ZLayer.succeed(repositoryMock)
  )
}
