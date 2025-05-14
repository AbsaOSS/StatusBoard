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
import za.co.absa.statusboard.model.NotificationAction.ColorLabels
import za.co.absa.statusboard.providers.EmailProvider
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio.test.{Spec, TestEnvironment, assertCompletes}
import zio.{Scope, ZIO, ZLayer}

object EmailNotificationActionerUnitTests extends ConfigProviderSpec {
  private val status = TestData.refinedStatus
  private val providerMock = mock(classOf[EmailProvider])

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("EmailNotificationActionerSuite")(
      test("Assemble default email from status and pass it to email provider") {
        for {
          action <- ZIO.attempt(NotificationAction.EMail(Seq("mail.one@test.me", "mail.two@test.me")))
          expectedSubject <- ZIO.succeed("Status Board Notification for Test Service [GREEN(Service is good)]")
          expectedBody <- ZIO.succeed(
            """Status Board Notification
              |Service: Test Service
              |Environment: TestEnv
              |Status: GREEN(Service is good)
              |Maintenance Message: Testing as usual
              |First Seen: 1989-05-29T12:00:00Z
              |Last Seen: 2024-05-17T12:00:00Z [PT306528H]""".stripMargin)
          _ <- EmailNotificationActioner.notify(action, status)
          _ <- ZIO.attempt(verify(providerMock, times(1)).sendEmail(Seq("mail.one@test.me", "mail.two@test.me"), expectedSubject, expectedBody))
        } yield assertCompletes
      },
      test("Assemble custom email from status and pass it to email provider") {
        for {
          action <- ZIO.attempt(NotificationAction.EMail(
            Seq("mail.one@test.me", "mail.two@test.me"),
            Some(" 0 {{ ENV }} 1 {{ SERVICE_NAME }} 2 {{ MAINTENANCE_MESSAGE }} 3 {{ STATUS_COLOR }} 4 {{ STATUS_MESSAGE }} 5 {{ STATUS_INTERMITTENT }} 6 {{ STATUS }} 7 {{ FIRST_SEEN }} 8 {{ LAST_SEEN }} 9 {{ DURATION_SEEN }} A {{ NOTIFICATION_SENT }}"),
            Some(Seq(
              "0 {{ ENV }}",
              "1 {{ SERVICE_NAME }}",
              "2 {{ MAINTENANCE_MESSAGE }}",
              "3 {{ STATUS_COLOR }}",
              "4 {{ STATUS_MESSAGE }}",
              "5 {{ STATUS_INTERMITTENT }}",
              "6 {{ STATUS }}",
              "7 {{ FIRST_SEEN }}",
              "8 {{ LAST_SEEN }}",
              "9 {{ DURATION_SEEN }}",
              "A {{ NOTIFICATION_SENT }}"
            ))
          ))
          expectedSubject <- ZIO.succeed(" 0 TestEnv 1 Test Service 2 Testing as usual 3 Green 4 Service is good 5 false 6 GREEN(Service is good) 7 1989-05-29T12:00:00Z 8 2024-05-17T12:00:00Z 9 PT306528H A false")
          expectedBody <- ZIO.succeed(
            """0 TestEnv
              |1 Test Service
              |2 Testing as usual
              |3 Green
              |4 Service is good
              |5 false
              |6 GREEN(Service is good)
              |7 1989-05-29T12:00:00Z
              |8 2024-05-17T12:00:00Z
              |9 PT306528H
              |A false""".stripMargin)
          _ <- EmailNotificationActioner.notify(action, status)
          _ <- ZIO.attempt(verify(providerMock, times(1)).sendEmail(Seq("mail.one@test.me", "mail.two@test.me"), expectedSubject, expectedBody))
        } yield assertCompletes
      },
      test("Assemble custom email with colorLabels from status and pass it to email provider") {
        for {
          action <- ZIO.attempt(NotificationAction.EMail(
            Seq("mail.one@test.me", "mail.two@test.me"),
            Some(" X {{ STATUS_COLOR_LABEL }}"),
            Some(Seq("X {{ STATUS_COLOR_LABEL }}"))
          ))
          expectedSubject <- ZIO.succeed(" X 🟩")
          expectedBody <- ZIO.succeed("X 🟩")
          _ <- EmailNotificationActioner.notify(action, status)
          _ <- ZIO.attempt(verify(providerMock, times(1)).sendEmail(Seq("mail.one@test.me", "mail.two@test.me"), expectedSubject, expectedBody))
        } yield assertCompletes
      },
      test("Assemble custom email with custom colorLabels from status and pass it to email provider") {
        for {
          action <- ZIO.attempt(NotificationAction.EMail(
            Seq("mail.one@test.me", "mail.two@test.me"),
            Some(" X {{ STATUS_COLOR_LABEL }}"),
            Some(Seq("X {{ STATUS_COLOR_LABEL }}")),
            Some(ColorLabels("redLbl", "amberLbl", "greenLbl", "blackLbl"))
          ))
          expectedSubject <- ZIO.succeed(" X greenLbl")
          expectedBody <- ZIO.succeed("X greenLbl")
          _ <- EmailNotificationActioner.notify(action, status)
          _ <- ZIO.attempt(verify(providerMock, times(1)).sendEmail(Seq("mail.one@test.me", "mail.two@test.me"), expectedSubject, expectedBody))
        } yield assertCompletes
      }
    )
  }.provide(EmailNotificationActionerImpl.layer, ZLayer.succeed(providerMock))
}
