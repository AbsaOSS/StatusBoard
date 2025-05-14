package za.co.absa.statusboard.model

import io.circe.generic.JsonCodec

import java.time.Instant

@JsonCodec
case class RefinedStatus(
  serviceName: String,
  env: String,
  status: RawStatus,
  maintenanceMessage: String,
  firstSeen: java.time.Instant = Instant.now(),
  lastSeen: java.time.Instant,
  notificationSent: Boolean
)
