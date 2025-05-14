package za.co.absa.statusboard.notification.actioners

import org.mockito.Mockito._
import za.co.absa.statusboard.model.NotificationAction
import za.co.absa.statusboard.providers.MsTeamsProvider
import za.co.absa.statusboard.testUtils.{ConfigProviderSpec, TestData}
import zio.test.Assertion.isUnit
import zio.test.{Spec, TestEnvironment, assertZIO}
import zio.{Scope, ZIO, ZLayer}

object MsTeamsNotificationActionerUnitTests extends ConfigProviderSpec {
  private val status = TestData.refinedStatus
  private val action = NotificationAction.MSTeams()
  private val providerMock = mock(classOf[MsTeamsProvider])
  private val expectedMessage =
    """
    {
      "type": "message",
      "attachments": [
        {
          "contentType": "application/vnd.microsoft.card.adaptive",
          "contentUrl": null,
          "content": {
            "$schema": "http://adaptivecards.io/schemas/adaptive-card.json",
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
                        "value": "Test Service"
                      },
                      {
                        "title": "📊 Service Environment:",
                        "value": "TestEnv"
                      },
                      {
                        "title": "🔍 Status:",
                        "value": "GREEN(Service is good)"
                      },
                      {
                        "title": "🔍 Maintenance Message:",
                        "value": "Testing as usual"
                      },
                      {
                        "title": "📅 First Seen:",
                        "value": "1989-05-29T12:00:00Z"
                      },
                      {
                        "title": "📅 Last Seen:",
                        "value": "2024-05-17T12:00:00Z [PT306528H]"
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

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("MSTeamsNotificationActionerSuite")(
      test("Assemble message from status and pass it to teams provider") {
        assertZIO(
          for {
            _ <- MsTeamsNotificationActioner.notify(action, status)
            _ <- ZIO.attempt(verify(providerMock, times(1)).sendMessage(expectedMessage))
          } yield ()
        )(isUnit)
      }
    )
  }.provide(MsTeamsNotificationActionerImpl.layer, ZLayer.succeed(providerMock))
}
