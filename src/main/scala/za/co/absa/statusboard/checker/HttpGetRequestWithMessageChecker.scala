package za.co.absa.statusboard.checker

import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio.UIO
import zio.macros.accessible

/**
 * Specialization of Checker for HttpGetRequestWithMessage
 */
@accessible
trait HttpGetRequestWithMessageChecker {
  def checkRawStatus(action: StatusCheckAction.HttpGetRequestWithMessage): UIO[RawStatus]
}
