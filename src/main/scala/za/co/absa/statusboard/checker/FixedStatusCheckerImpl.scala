package za.co.absa.statusboard.checker

import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio.{UIO, ZIO, ZLayer}

class FixedStatusCheckerImpl extends FixedStatusChecker {
  override def checkRawStatus(action: StatusCheckAction.FixedStatus): UIO[RawStatus] = ZIO.succeed(action.status)
}

object FixedStatusCheckerImpl {
  val layer: ZLayer[Any, Throwable, FixedStatusChecker] = ZLayer.succeed(new FixedStatusCheckerImpl())
}
