package za.co.absa.statusboard.config

import zio.Config
import zio.config.magnolia.deriveConfig

case class ServerConfig(
  port: Int,
  apiKey: String
)

object ServerConfig {
  val config: Config[ServerConfig] = deriveConfig[ServerConfig].nested("server")
}
