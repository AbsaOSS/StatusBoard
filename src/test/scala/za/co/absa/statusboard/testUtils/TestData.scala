package za.co.absa.statusboard.testUtils

import za.co.absa.statusboard.model.ServiceConfiguration.ServiceConfigurationLinks
import za.co.absa.statusboard.model._

import java.time.Instant

object TestData {
  val rawStatusRed: RawStatus = RawStatus.Red("Service is FUBAR", intermittent = false)
  val rawStatusRedIntermittent: RawStatus = RawStatus.Red("Service is FUBAR", intermittent = true)
  val rawStatusAmber: RawStatus = RawStatus.Amber("Service is struggling", intermittent = false)
  val rawStatusAmberIntermittent: RawStatus = RawStatus.Amber("Service is struggling", intermittent = true)
  val rawStatusGreen: RawStatus = RawStatus.Green("Service is good")

  val refinedStatus: RefinedStatus = RefinedStatus(
    serviceName = "Test Service",
    env = "TestEnv",
    status = rawStatusGreen,
    maintenanceMessage = "Testing as usual",
    firstSeen = Instant.parse("1989-05-29T12:00:00Z"),
    lastSeen = Instant.parse("2024-05-17T12:00:00Z"),
    notificationSent = false
  )

  val serviceConfiguration: ServiceConfiguration = ServiceConfiguration(
    name = "Test Service",
    env = "TestEnv",
    hidden = false,
    snowID = "000",
    description = "Testing service",
    maintenanceMessage = "Testing as usual",
    links = ServiceConfigurationLinks("homeURL", "snowURL", "supportURL", "documentationURL", "githubURL"),
    statusCheckAction = StatusCheckAction.HttpGetRequestWithMessage("testMe", "i.dont.exist", 0, "neverExisted"),
    statusCheckIntervalSeconds = 5,
    statusCheckNonGreenIntervalSeconds = 3,
    notificationCondition = NotificationCondition.DurationBased(5),
    notificationAction = Seq(
      NotificationAction.EMail(Seq("mail.no-one.test", "also.no-one.test")),
      NotificationAction.MSTeams(),
      NotificationAction.Repository()
    )
  )
}
