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
import io.circe.syntax.EncoderOps
import za.co.absa.statusboard.model.{NotificationAction, RefinedStatus}
import za.co.absa.statusboard.providers.{EmailProvider, MsTeamsProvider}
import zio.{Task, ZIO, ZLayer}

import java.time.{Duration, Instant}

class MsTeamsNotificationActionerImpl(msTeamsProvider: MsTeamsProvider) extends MsTeamsNotificationActioner {
  override def notify(action: NotificationAction.MSTeams, status: RefinedStatus): Task[Unit] = {
    val message = s"""
    {
      "type": "message",
      "attachments": [
        {
          "contentType": "application/vnd.microsoft.card.adaptive",
          "contentUrl": null,
          "content": {
            "$$schema": "http://adaptivecards.io/schemas/adaptive-card.json",
            "type": "AdaptiveCard",
            "version": "1.5",
            "minHeight": "100px",
            "body": [
              {
                "type": "Container",
                "height": "stretch",
                "separator": true,
                "bleed": true,
                "items": [
                  {
                    "type": "TextBlock",
                    "text": "Status Board Notification",
                    "wrap": false
                  },
                  {
                    "type": "FactSet",
                    "facts": [
                      {
                        "title": "📊 Service Name:",
                        "value": "${status.serviceName}"
                      },
                      {
                        "title": "📊 Service Environment:",
                        "value": "${status.env}"
                      },
                      {
                        "title": "🔍 Status:",
                        "value": "${status.status.toString.replace("\"", "\\\"")}"
                      },
                      {
                        "title": "🔍 Maintenance Message:",
                        "value": "${status.maintenanceMessage}"
                      },
                      {
                        "title": "📅 First Seen:",
                        "value": "${status.firstSeen}"
                      },
                      {
                        "title": "📅 Last Seen:",
                        "value": "${status.lastSeen} [${Duration.between(status.firstSeen, status.lastSeen)}]"
                      }
                    ]
                  }
                ]
              }
            ]
          }
        }
      ]
    }
    """.stripMargin

    msTeamsProvider.sendMessage(message)
  }
}

object MsTeamsNotificationActionerImpl {
  val layer: ZLayer[MsTeamsProvider, Throwable, MsTeamsNotificationActioner] = ZLayer {
    for {
      msTeamsProvider <- ZIO.service[MsTeamsProvider]
    } yield new MsTeamsNotificationActionerImpl(msTeamsProvider)
  }
}
