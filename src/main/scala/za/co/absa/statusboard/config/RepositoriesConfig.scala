package za.co.absa.statusboard.config

import zio.Config
import zio.config.magnolia.deriveConfig

case class RepositoriesConfig(
  configurationsTableName: String,
  notificationsTableName: String,
  statusesTableName: String
)

object RepositoriesConfig {
  val config: Config[RepositoriesConfig] = deriveConfig[RepositoriesConfig].nested("repositories")
}
