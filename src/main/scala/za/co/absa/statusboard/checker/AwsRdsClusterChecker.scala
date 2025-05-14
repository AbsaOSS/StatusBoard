package za.co.absa.statusboard.checker

import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio.UIO
import zio.macros.accessible

/**
 * Specialization of Checker for AwsRds
 */
@accessible
trait AwsRdsClusterChecker {
  def checkRawStatus(action: StatusCheckAction.AwsRdsCluster): UIO[RawStatus]
}
