package za.co.absa.statusboard.checker

import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio.UIO
import zio.macros.accessible

/**
 * Specialization of Checker for HttpGetRequestStatusCodeOnly
 */
@accessible
trait HttpGetRequestStatusCodeOnlyChecker {
  def checkRawStatus(action: StatusCheckAction.HttpGetRequestStatusCodeOnly): UIO[RawStatus]
}
