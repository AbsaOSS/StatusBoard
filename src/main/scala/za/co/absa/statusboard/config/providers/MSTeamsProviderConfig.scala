package za.co.absa.statusboard.config.providers

import zio.Config
import zio.config.magnolia.deriveConfig

case class MSTeamsProviderConfig(webhookUrl: String)

object MSTeamsProviderConfig {
  val config: Config[MSTeamsProviderConfig] = deriveConfig[MSTeamsProviderConfig].nested("providers", "msTeams")
}
