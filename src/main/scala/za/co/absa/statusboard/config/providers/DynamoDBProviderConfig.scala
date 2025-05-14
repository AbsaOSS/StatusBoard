package za.co.absa.statusboard.config.providers

import zio.Config
import zio.config.magnolia.deriveConfig

case class DynamoDBProviderConfig(endpointOverride: Option[String])

object DynamoDBProviderConfig {
  val config: Config[DynamoDBProviderConfig] = deriveConfig[DynamoDBProviderConfig].nested("providers", "dynamoDB")
}
