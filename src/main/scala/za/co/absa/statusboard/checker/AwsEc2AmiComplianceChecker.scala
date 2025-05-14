package za.co.absa.statusboard.checker

import za.co.absa.statusboard.model.{RawStatus, StatusCheckAction}
import zio.UIO
import zio.macros.accessible

/**
 * Specialization of Checker for AwsEc2AmiCompliant
 */
@accessible
trait AwsEc2AmiComplianceChecker {
  def checkRawStatus(action: StatusCheckAction.AwsEc2AmiCompliance): UIO[RawStatus]
}
