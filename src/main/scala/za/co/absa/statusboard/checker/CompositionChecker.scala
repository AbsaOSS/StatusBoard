package za.co.absa.statusboard.checker

import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio.UIO
import zio.macros.accessible

/**
 * Specialization of Checker for FixedStatus
 */
@accessible
trait CompositionChecker {
  def checkRawStatus(action: StatusCheckAction.Composition): UIO[RawStatus]
}
