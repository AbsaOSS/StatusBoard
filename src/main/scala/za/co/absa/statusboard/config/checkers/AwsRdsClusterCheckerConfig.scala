package za.co.absa.statusboard.config.checkers

import zio.Config
import zio.config.magnolia.deriveConfig

case class AwsRdsClusterCheckerConfig(green: Seq[String], amber: Seq[String], red: Seq[String])

object AwsRdsClusterCheckerConfig {
  val config: Config[AwsRdsClusterCheckerConfig] = deriveConfig[AwsRdsClusterCheckerConfig].nested("checkers", "awsRdsCluster")
}
