package za.co.absa.statusboard.model

import io.circe.generic.JsonCodec
import za.co.absa.statusboard.model.ServiceConfiguration.ServiceConfigurationLinks

@JsonCodec
case class ServiceConfiguration (
  name: String, // Part of composite PK
  env: String, // Part of composite PK
  hidden: Boolean,
  snowID: String,
  description: String,
  maintenanceMessage: String,
  links: ServiceConfigurationLinks,
  statusCheckAction: StatusCheckAction,
  statusCheckIntervalSeconds: Int,
  statusCheckNonGreenIntervalSeconds: Int,
  notificationCondition: NotificationCondition,
  notificationAction: Seq[NotificationAction]
)

object ServiceConfiguration {
  @JsonCodec
  case class ServiceConfigurationLinks(
    home: String,
    snow: String,
    support: String,
    documentation: String,
    github: String
  )
}
