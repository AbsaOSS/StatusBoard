package za.co.absa.statusboard.checker

import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio.UIO
import zio.macros.accessible

/**
 * Specialization of Checker for HttpGetRequestWithJsonStatus
 */
@accessible
trait HttpGetRequestWithJsonStatusChecker {
  def checkRawStatus(action: StatusCheckAction.HttpGetRequestWithJsonStatus): UIO[RawStatus]
}
