package za.co.absa.statusboard.notification.actioners
import za.co.absa.statusboard.model.NotificationAction.ColorLabels
import za.co.absa.statusboard.model.{NotificationAction, RawStatus, RefinedStatus}
import za.co.absa.statusboard.notification.actioners.EmailNotificationActionerImpl.{defaultBody, defaultColorLabels, defaultSubject}
import za.co.absa.statusboard.providers.EmailProvider
import za.co.absa.statusboard.utils.JinjaLikeTemplating
import zio.{Task, ZIO, ZLayer}

import java.time.Duration

class EmailNotificationActionerImpl(emailProvider: EmailProvider) extends EmailNotificationActioner {
  override def notify(action: NotificationAction.EMail, status: RefinedStatus): Task[Unit] = {
    val subjectTemplate = action.subject match {
      case None => defaultSubject
      case Some(customSubject) => customSubject
    }
    val bodyTemplate = action.body match {
      case None => defaultBody
      case Some(customBodyLines) => customBodyLines.mkString("\n")
    }
    val colorLabels = action.colorLabels match {
      case None => defaultColorLabels
      case Some(colorLabels) => colorLabels
    }
    val colorLabel = status.status match {
      case RawStatus.Red(_, _) => colorLabels.red
      case RawStatus.Amber(_, _) => colorLabels.amber
      case RawStatus.Green(_) => colorLabels.green
      case RawStatus.Black() => colorLabels.black
    }
    val vars = Map(
      "ENV" -> status.env,
      "SERVICE_NAME" -> status.serviceName,
      "MAINTENANCE_MESSAGE" -> status.maintenanceMessage,
      "STATUS_COLOR" -> status.status.color,
      "STATUS_COLOR_LABEL" -> colorLabel,
      "STATUS_MESSAGE" -> status.status.statusMessage,
      "STATUS_INTERMITTENT" -> status.status.intermittent.toString,
      "STATUS" -> status.status.toString,
      "FIRST_SEEN" -> status.firstSeen.toString,
      "LAST_SEEN" -> status.lastSeen.toString,
      "DURATION_SEEN" -> Duration.between(status.firstSeen, status.lastSeen).toString,
      "NOTIFICATION_SENT" -> status.notificationSent.toString
    )
    emailProvider.sendEmail(
      action.addresses,
      JinjaLikeTemplating.renderTemplate(subjectTemplate, vars),
      JinjaLikeTemplating.renderTemplate(bodyTemplate, vars))
  }
}

object EmailNotificationActionerImpl {
  private val defaultSubject = "Status Board Notification for {{ SERVICE_NAME }} [{{ STATUS }}]"
  private val defaultBody = """Status Board Notification
                               |Service: {{ SERVICE_NAME }}
                               |Environment: {{ ENV }}
                               |Status: {{ STATUS }}
                               |Maintenance Message: {{ MAINTENANCE_MESSAGE }}
                               |First Seen: {{ FIRST_SEEN }}
                               |Last Seen: {{ LAST_SEEN }} [{{ DURATION_SEEN }}]""".stripMargin
  private val defaultColorLabels = ColorLabels("🟥", "🟨", "🟩", "⬛")

  val layer: ZLayer[EmailProvider, Throwable, EmailNotificationActioner] = ZLayer {
    for {
      emailProvider <- ZIO.service[EmailProvider]
    } yield new EmailNotificationActionerImpl(emailProvider)
  }
}
