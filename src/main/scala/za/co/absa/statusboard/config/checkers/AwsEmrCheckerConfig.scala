package za.co.absa.statusboard.config.checkers

import zio.Config
import zio.config.magnolia.deriveConfig

case class AwsEmrCheckerConfig(green: Seq[String], amber: Seq[String], red: Seq[String], nodeUp: Seq[String], nodeIgnored: Seq[String], nodeFailed: Seq[String])

object AwsEmrCheckerConfig {
  val config: Config[AwsEmrCheckerConfig] = deriveConfig[AwsEmrCheckerConfig].nested("checkers", "awsEmr")
}
