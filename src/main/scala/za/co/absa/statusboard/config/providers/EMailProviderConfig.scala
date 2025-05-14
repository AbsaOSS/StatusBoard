package za.co.absa.statusboard.config.providers

import zio.Config
import zio.config.magnolia.deriveConfig

case class EMailProviderConfig(smtpHost: String, senderAddress: String)

object EMailProviderConfig {
  val config: Config[EMailProviderConfig] = deriveConfig[EMailProviderConfig].nested("providers", "email")
}
