package za.co.absa.statusboard.checker

import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio.UIO
import zio.macros.accessible

/**
 * Specialization of Checker for FixedStatus
 */
@accessible
trait FixedStatusChecker {
  def checkRawStatus(action: StatusCheckAction.FixedStatus): UIO[RawStatus]
}
