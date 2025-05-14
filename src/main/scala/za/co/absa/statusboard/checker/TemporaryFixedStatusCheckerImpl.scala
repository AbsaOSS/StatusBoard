package za.co.absa.statusboard.checker

import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio.{UIO, ZIO, ZLayer}

class TemporaryFixedStatusCheckerImpl extends TemporaryFixedStatusChecker {
  override def checkRawStatus(action: StatusCheckAction.TemporaryFixedStatus): UIO[RawStatus] = ZIO.succeed(action.status)
}

object TemporaryFixedStatusCheckerImpl {
  val layer: ZLayer[Any, Throwable, TemporaryFixedStatusChecker] = ZLayer.succeed(new TemporaryFixedStatusCheckerImpl())
}
