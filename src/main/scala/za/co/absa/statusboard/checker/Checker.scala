package za.co.absa.statusboard.checker

import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio.UIO
import zio.macros.accessible

@accessible
trait Checker {
  /**
   * Checks RAW status of a specific service.
   *
   * @param action Configuration of status-checking action to be performed
   * @return A [[zio.UIO]] that will produce a [[za.co.absa.statusboard.model.RawStatus]] of a service,
   */
  def checkRawStatus(action: StatusCheckAction): UIO[RawStatus]
}
