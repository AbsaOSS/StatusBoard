package za.co.absa.statusboard.checker

import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio.UIO
import zio.macros.accessible

/**
 * Specialization of Checker for TemporaryFixedStatus
 */
@accessible
trait TemporaryFixedStatusChecker {
  def checkRawStatus(action: StatusCheckAction.TemporaryFixedStatus): UIO[RawStatus]
}
