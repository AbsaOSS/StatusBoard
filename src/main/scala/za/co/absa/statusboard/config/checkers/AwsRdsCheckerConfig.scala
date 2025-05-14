package za.co.absa.statusboard.config.checkers

import zio.Config
import zio.config.magnolia.deriveConfig

case class AwsRdsCheckerConfig(green: Seq[String], amber: Seq[String], red: Seq[String])

object AwsRdsCheckerConfig {
  val config: Config[AwsRdsCheckerConfig] = deriveConfig[AwsRdsCheckerConfig].nested("checkers", "awsRds")
}
