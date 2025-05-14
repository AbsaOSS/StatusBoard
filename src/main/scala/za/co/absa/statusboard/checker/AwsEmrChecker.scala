package za.co.absa.statusboard.checker

import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio.UIO
import zio.macros.accessible

/**
 * Specialization of Checker for AwsEmr
 */
@accessible
trait AwsEmrChecker {
  def checkRawStatus(action: StatusCheckAction.AwsEmr): UIO[RawStatus]
}
